package com.nestnote.app.voicenote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nestnote.app.VoiceAttachmentManifest
import com.nestnote.app.VoiceAttachmentStore
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceNoteRepositoryTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    private fun repository(root: File, key: javax.crypto.SecretKey = newKey()): VoiceNoteRepository {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return VoiceNoteRepository(
            VoiceAttachmentStore(context, { key }, root),
            VoiceAttachmentManifest(context, root)
        )
    }

    @Test
    fun saveListsAndReadsOnlyCommittedEncryptedAttachment() {
        val root = Files.createTempDirectory("nestnote-voice-repository").toFile()
        val repository = repository(root)
        val plaintext = "encrypted voice payload".toByteArray()

        val metadata = repository.save(
            noteId = 51L,
            attachmentId = "clip_51",
            input = ByteArrayInputStream(plaintext),
            createdAtMillis = 123L,
            durationMillis = 456L,
            displayName = "Meeting note"
        )

        assertEquals(listOf(metadata), repository.list(51L))
        assertEquals(plaintext.toList(), repository.read(51L, "clip_51").toList())
        assertTrue(File(File(root, "51"), "clip_51.nva").isFile)
        assertFalse(
            File(File(root, "51"), "clip_51.nva").readBytes()
                .toString(Charsets.ISO_8859_1).contains("encrypted voice payload")
        )
    }

    @Test
    fun metadataCommitFailureRemovesOnlyNewPayload() {
        val root = Files.createTempDirectory("nestnote-voice-repository-failed-index").toFile()
        File(root, "manifest_v1.json").writeText("{ corrupt manifest")
        val repository = repository(root)

        try {
            repository.save(
                noteId = 52L,
                attachmentId = "must_not_publish",
                input = ByteArrayInputStream(byteArrayOf(1, 2, 3)),
                createdAtMillis = 1L,
                durationMillis = 1L,
                displayName = "Recording"
            )
            throw AssertionError("A corrupt manifest must prevent save")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("could not be committed"))
        }

        assertTrue(repository.listPayloads(52L).isEmpty())
        assertFalse(File(File(root, "52"), "must_not_publish.nva").exists())
    }

    @Test
    fun deleteAllForOneNotePreservesOtherNotes() {
        val root = Files.createTempDirectory("nestnote-voice-repository-delete").toFile()
        val key = newKey()
        val repository = repository(root, key)
        val retainedBytes = byteArrayOf(9, 8, 7)
        repository.save(53L, "remove_me", ByteArrayInputStream(byteArrayOf(1, 2)), 1L, 1L, "Temporary")
        repository.save(54L, "keep_me", ByteArrayInputStream(retainedBytes), 2L, 2L, "Saved")

        repository.deleteAllForNote(53L)

        assertTrue(repository.listPayloads(53L).isEmpty())
        assertTrue(repository.list(53L).isEmpty())
        assertArrayEquals(retainedBytes, repository.read(54L, "keep_me"))
        assertEquals(listOf("keep_me"), repository.list(54L).map { it.attachmentId })
    }

    @Test
    fun orphanPayloadRemainsDiscoverableWhenManifestEntryIsMissing() {
        val root = Files.createTempDirectory("nestnote-voice-repository-orphan").toFile()
        val repository = repository(root)
        repository.save(55L, "indexed", ByteArrayInputStream(byteArrayOf(1, 2)), 1L, 1L, "Indexed")
        val manifestFile = File(root, "manifest_v1.json")
        manifestFile.writeText("""{"schemaVersion":1,"attachments":[]}""")

        assertTrue(repository.list(55L).isEmpty())
        assertEquals(listOf("indexed"), repository.listPayloads(55L).map { it.attachmentId })
        assertTrue(File(File(root, "55"), "indexed.nva").isFile)
    }

    @Test
    fun corruptedCiphertextIsRejectedByRepositoryRead() {
        val root = Files.createTempDirectory("nestnote-voice-corrupt-ciphertext").toFile()
        val repository = repository(root)
        repository.save(
            noteId = 56L,
            attachmentId = "tampered_clip",
            input = ByteArrayInputStream("must authenticate before returning".toByteArray()),
            createdAtMillis = 1L,
            durationMillis = 1L,
            displayName = "Tampered"
        )

        val payload = File(File(root, "56"), "tampered_clip.nva")
        val encrypted = payload.readBytes()
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 0x01).toByte()
        payload.writeBytes(encrypted)

        try {
            repository.read(56L, "tampered_clip")
            throw AssertionError("Tampered AES-GCM payload must not be returned")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("authentication failed"))
        }
    }


    @Test
    fun duplicateAttachmentIdDoesNotReplaceExistingCiphertext() {
        val root = Files.createTempDirectory("nestnote-voice-duplicate-id").toFile()
        val repository = repository(root)
        val original = "original recording".toByteArray()
        repository.save(57L, "same_id", ByteArrayInputStream(original), 1L, 1L, "Original")

        try {
            repository.save(57L, "same_id", ByteArrayInputStream("replacement".toByteArray()), 2L, 2L, "Replacement")
            throw AssertionError("Duplicate attachment identity must be rejected")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("already exists"))
        }

        assertArrayEquals(original, repository.read(57L, "same_id"))
        assertEquals("Original", repository.list(57L).single().displayName)
    }

    @Test
    fun missingKeystoreKeyFailsClosedAndPreservesCiphertext() {
        val root = Files.createTempDirectory("nestnote-voice-missing-key").toFile()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val originalKey = newKey()
        val writer = repository(root, originalKey)
        writer.save(58L, "key_lost", ByteArrayInputStream(byteArrayOf(4, 5, 6)), 1L, 1L, "Key lost")
        val payload = File(File(root, "58"), "key_lost.nva")
        val before = payload.readBytes()
        val missingKeyStore = VoiceAttachmentStore(context, {
            throw IOException("Voice-note encryption key is missing")
        }, root)
        val reader = VoiceNoteRepository(missingKeyStore, VoiceAttachmentManifest(context, root))

        try {
            reader.read(58L, "key_lost")
            throw AssertionError("A missing key must not be recreated or return plaintext")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("key is missing"))
        }

        assertArrayEquals(before, payload.readBytes())
    }

    @Test
    fun reconciliationNeverDeletesUnindexedCiphertext() {
        val root = Files.createTempDirectory("nestnote-voice-safe-reconcile").toFile()
        val repository = repository(root)
        repository.save(59L, "recover_me", ByteArrayInputStream(byteArrayOf(7, 8, 9)), 1L, 1L, "Recovery")
        File(root, "manifest_v1.json").writeText("""{"schemaVersion":1,"attachments":[]}""")
        val payload = File(File(root, "59"), "recover_me.nva")
        val before = payload.readBytes()

        assertEquals(0, repository.reconcile(setOf(59L)))
        assertArrayEquals(before, payload.readBytes())
        assertEquals(listOf("recover_me"), repository.listPayloads(59L).map { it.attachmentId })
    }

    @Test
    fun interruptedSaveTemporaryFileIsCleanedWithoutTouchingCommittedPayloads() {
        val root = Files.createTempDirectory("nestnote-voice-interrupted-save").toFile()
        val noteDir = File(root, "60").apply { mkdirs() }
        val committed = File(noteDir, "committed.nva").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val staleTemp = File(root, ".60_interrupted.aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.tmp").apply { writeText("partial ciphertext") }

        val context = ApplicationProvider.getApplicationContext<Context>()
        VoiceAttachmentStore(context, { newKey() }, root)

        assertFalse(staleTemp.exists())
        assertTrue(committed.exists())
        assertArrayEquals(byteArrayOf(1, 2, 3), committed.readBytes())
    }


    @Test
    fun reviewPayloadIsEncryptedButNotListedUntilExplicitlyAttached() {
        val root = Files.createTempDirectory("nestnote-voice-review-lifecycle").toFile()
        val repository = repository(root)
        val original = "review before attach".toByteArray()

        repository.saveForReview(61L, "review_clip", ByteArrayInputStream(original))

        assertTrue(File(File(root, "61"), "review_clip.nva").isFile)
        assertTrue(repository.list(61L).isEmpty())
        assertEquals(listOf("review_clip"), repository.listPayloads(61L).map { it.attachmentId })
        assertArrayEquals(original, repository.read(61L, "review_clip"))

        repository.attachExisting(
            noteId = 61L,
            attachmentId = "review_clip",
            createdAtMillis = 10L,
            durationMillis = 20L,
            displayName = "Review clip"
        )

        assertEquals("Review clip", repository.list(61L).single().displayName)
        repository.delete(61L, "review_clip")
        assertTrue(repository.listPayloads(61L).isEmpty())
        assertTrue(repository.list(61L).isEmpty())
    }

    @Test
    fun discardingReviewPayloadDoesNotPublishMetadata() {
        val root = Files.createTempDirectory("nestnote-voice-review-discard").toFile()
        val repository = repository(root)
        repository.saveForReview(62L, "discard_me", ByteArrayInputStream(byteArrayOf(1, 3, 5)))

        repository.deletePayload(62L, "discard_me")

        assertTrue(repository.listPayloads(62L).isEmpty())
        assertTrue(repository.list(62L).isEmpty())
    }

    @Test
    fun savePlainCommitsMetadataAndExposesPlaybackFileWithoutEncryption() {
        val root = Files.createTempDirectory("nestnote-voice-repository-plain").toFile()
        val repository = repository(root)
        val bytes = "unencrypted large music file".toByteArray()

        val metadata = repository.savePlain(
            noteId = 63L,
            attachmentId = "music_63",
            input = ByteArrayInputStream(bytes),
            createdAtMillis = 5L,
            durationMillis = 9_000L,
            displayName = "Big song.mp3"
        )

        assertEquals(listOf(metadata), repository.list(63L))
        assertEquals("Big song.mp3", metadata.displayName)
        assertTrue(File(File(root, "63"), "music_63.nvap").isFile)
        assertArrayEquals(bytes, File(File(root, "63"), "music_63.nvap").readBytes())
        val playback = repository.plainPlaybackFile(63L, "music_63")
        assertTrue(playback != null && playback.isFile)
        // The byte API must refuse plain attachments so playback always streams from disk.
        try {
            repository.read(63L, "music_63")
            throw AssertionError("Plain attachments must not be returned through the byte API")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("without encryption"))
        }
        repository.delete(63L, "music_63")
        assertTrue(repository.listPayloads(63L).isEmpty())
        assertTrue(repository.list(63L).isEmpty())
        assertEquals(null, repository.plainPlaybackFile(63L, "music_63"))
    }

}
