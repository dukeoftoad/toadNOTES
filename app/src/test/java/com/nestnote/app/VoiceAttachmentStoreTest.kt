package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
class VoiceAttachmentStoreTest {
    private fun key() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private fun store(root: File = Files.createTempDirectory("nestnote-voice-test").toFile(), secret: javax.crypto.SecretKey = key()) =
        VoiceAttachmentStore(ApplicationProvider.getApplicationContext<Context>(), { secret }, root)

    @Test fun encryptedFileRoundTripsAndDoesNotContainPlaintext() {
        val root = Files.createTempDirectory("nestnote-voice-roundtrip").toFile()
        val store = store(root)
        val bytes = "private voice sample payload".toByteArray()
        val result = store.save(7L, "clip_1", ByteArrayInputStream(bytes))
        val file = File(File(root, "7"), "clip_1.nva")
        assertTrue(result.encryptedSizeBytes > bytes.size)
        assertTrue(file.isFile)
        assertFalse(file.readBytes().toString(Charsets.ISO_8859_1).contains("private voice sample payload"))
        assertArrayEquals(bytes, store.read(7L, "clip_1"))
        assertEquals(listOf("clip_1"), store.list(7L).map { it.attachmentId })
    }

    @Test fun tamperedCiphertextFailsClosed() {
        val root = Files.createTempDirectory("nestnote-voice-tamper").toFile()
        val store = store(root)
        store.save(9L, "clip", ByteArrayInputStream(ByteArray(2048) { it.toByte() }))
        val file = File(File(root, "9"), "clip.nva")
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 0x40).toByte()
        file.writeBytes(bytes)
        try { store.read(9L, "clip"); throw AssertionError("Tampered ciphertext must not decrypt") }
        catch (expected: IOException) { assertTrue(expected.message!!.contains("authentication")) }
    }

    @Test fun attachmentIdentityIsAuthenticated() {
        val store = store()
        store.save(10L, "clip", ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))
        try { store.read(11L, "clip"); throw AssertionError("Wrong note identity must fail") }
        catch (expected: IOException) { assertTrue(expected.message!!.contains("authentication") || expected.message!!.contains("not found")) }
    }

    @Test fun wrongKeyCannotReadAttachment() {
        val root = Files.createTempDirectory("nestnote-voice-wrong-key").toFile()
        store(root).save(12L, "clip", ByteArrayInputStream(byteArrayOf(5, 6, 7)))
        try { store(root, key()).read(12L, "clip"); throw AssertionError("Wrong key must not decrypt") }
        catch (expected: IOException) { assertTrue(expected.message!!.contains("authentication")) }
    }

    @Test fun failedInputDoesNotPublishFileOrLeaveTemporaryCiphertext() {
        val root = Files.createTempDirectory("nestnote-voice-failure").toFile()
        val store = store(root)
        val broken = object : java.io.InputStream() { override fun read(): Int = throw IOException("simulated input failure") }
        try { store.save(13L, "clip", broken); throw AssertionError("Expected input failure") }
        catch (expected: IOException) { assertEquals("simulated input failure", expected.message) }
        assertTrue(store.list(13L).isEmpty())
        assertEquals(0, File(root, "13").listFiles()?.size ?: 0)
    }

    @Test fun rejectsInvalidIdentitiesAndDuplicateWrites() {
        val store = store()
        val payload = byteArrayOf(1, 2, 3)
        try { store.save(0L, "clip", ByteArrayInputStream(payload)); throw AssertionError("Invalid note ID") }
        catch (expected: IllegalArgumentException) { }
        try { store.save(1L, "../escape", ByteArrayInputStream(payload)); throw AssertionError("Path traversal ID") }
        catch (expected: IllegalArgumentException) { }
        store.save(1L, "clip", ByteArrayInputStream(payload))
        try { store.save(1L, "clip", ByteArrayInputStream(payload)); throw AssertionError("Duplicate ID must not overwrite") }
        catch (expected: IOException) { assertTrue(expected.message!!.contains("already exists")) }
    }

    @Test fun deleteIsIdempotentAndOnlyRemovesSelectedAttachment() {
        val store = store()
        store.save(14L, "first", ByteArrayInputStream(byteArrayOf(1)))
        store.save(14L, "second", ByteArrayInputStream(byteArrayOf(2)))
        assertTrue(store.delete(14L, "first"))
        assertTrue(store.delete(14L, "first"))
        assertEquals(listOf("second"), store.list(14L).map { it.attachmentId })
    }


    @Test fun deleteAllRemovesOnlySelectedNoteAttachments() {
        val root = Files.createTempDirectory("nestnote-voice-delete-all").toFile()
        val store = store(root)
        store.save(21L, "one", ByteArrayInputStream(byteArrayOf(1)))
        store.save(21L, "two", ByteArrayInputStream(byteArrayOf(2)))
        store.save(22L, "keep", ByteArrayInputStream(byteArrayOf(3)))

        assertTrue(store.deleteAll(21L))
        assertTrue(store.list(21L).isEmpty())
        assertEquals(listOf("keep"), store.list(22L).map { it.attachmentId })
        assertTrue(store.deleteAll(21L))
    }

    @Test fun orphanCleanupOnlyRemovesNumericDirectoriesNotInLiveSet() {
        val root = Files.createTempDirectory("nestnote-voice-orphans").toFile()
        val store = store(root)
        store.save(31L, "live", ByteArrayInputStream(byteArrayOf(1)))
        store.save(32L, "orphan", ByteArrayInputStream(byteArrayOf(2)))
        File(root, "unrelated").mkdirs()

        assertEquals(1, store.removeOrphans(setOf(31L)))
        assertEquals(listOf("live"), store.list(31L).map { it.attachmentId })
        assertTrue(store.list(32L).isEmpty())
        assertTrue(File(root, "unrelated").isDirectory)
    }

    @Test fun orphanCleanupRejectsInvalidLiveIds() {
        val store = store()
        try { store.removeOrphans(setOf(0L)); throw AssertionError("Invalid IDs must be rejected") }
        catch (expected: IllegalArgumentException) { }
    }

    @Test fun importAndRecordingSizeLimitsAreExplicit() {
        assertEquals(100 * 1024 * 1024, VoiceAttachmentStore.MAX_AUDIO_BYTES)
        assertEquals(25 * 1024 * 1024, com.nestnote.app.voicenote.VoiceNoteRepository.MAX_RECORDING_BYTES)
        assertEquals(100 * 1024 * 1024, com.nestnote.app.voicenote.VoiceNoteRepository.MAX_IMPORT_BYTES)
    }

    @Test fun oversizedInputIsRejectedWithoutPublishingFile() {
        val root = Files.createTempDirectory("nestnote-voice-large").toFile()
        val store = store(root)
        val oversized = object : java.io.InputStream() {
            private var remaining = VoiceAttachmentStore.MAX_AUDIO_BYTES.toLong() + 1
            override fun read(): Int { if (remaining <= 0) return -1; remaining--; return 0 }
        }
        try { store.save(15L, "large", oversized); throw AssertionError("Oversized attachment must fail") }
        catch (expected: IOException) { assertTrue(expected.message!!.contains("size limit")) }
        assertTrue(store.list(15L).isEmpty())
    }

    @Test fun plainAttachmentStreamsToDiskUnencryptedAndListsAsPlain() {
        val root = Files.createTempDirectory("nestnote-voice-plain").toFile()
        val store = store(root)
        val bytes = ByteArray(4096) { (it % 251).toByte() }

        val result = store.savePlain(71L, "big_clip", ByteArrayInputStream(bytes))

        assertTrue(result.plain)
        assertEquals(bytes.size.toLong(), result.encryptedSizeBytes)
        val file = File(File(root, "71"), "big_clip.nvap")
        assertTrue(file.isFile)
        assertArrayEquals(bytes, file.readBytes())
        val listed = store.list(71L).single()
        assertEquals("big_clip", listed.attachmentId)
        assertTrue(listed.plain)
        assertEquals(bytes.size.toLong(), listed.encryptedSizeBytes)
        val playback = store.plainFile(71L, "big_clip")
        assertTrue(playback != null && playback.isFile)
    }

    @Test fun plainAttachmentIsNotReadableThroughAuthenticatedByteApi() {
        val store = store()
        store.savePlain(72L, "plain_clip", ByteArrayInputStream(byteArrayOf(1, 2, 3, 4)))
        try {
            store.read(72L, "plain_clip")
            throw AssertionError("Plain attachments must be read from their file, not the byte API")
        } catch (expected: IOException) {
            assertTrue(expected.message!!.contains("without encryption"))
        }
    }

    @Test fun deleteRemovesPlainAttachmentAndIsIdempotent() {
        val store = store()
        store.savePlain(73L, "plain_clip", ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        assertTrue(store.delete(73L, "plain_clip"))
        assertTrue(store.delete(73L, "plain_clip"))
        assertTrue(store.list(73L).isEmpty())
        assertEquals(null, store.plainFile(73L, "plain_clip"))
    }

    @Test fun copyAllStreamsPlainAttachmentFileToFileAndReencryptsEncryptedOnes() {
        val root = Files.createTempDirectory("nestnote-voice-copy-plain").toFile()
        val store = store(root)
        val plainBytes = ByteArray(2048) { (it % 97).toByte() }
        val secretBytes = "streamed copy keeps secrets".toByteArray()
        store.savePlain(81L, "music_file", ByteArrayInputStream(plainBytes))
        store.save(81L, "voice_clip", ByteArrayInputStream(secretBytes))

        val copied = store.copyAll(81L, 82L)

        assertEquals(listOf("music_file", "voice_clip"), copied)
        val copiedPlain = store.list(82L).first { it.attachmentId == "music_file" }
        assertTrue(copiedPlain.plain)
        assertArrayEquals(plainBytes, File(File(root, "82"), "music_file.nvap").readBytes())
        assertArrayEquals(secretBytes, store.read(82L, "voice_clip"))
        // The plain copy must not expose plaintext through the byte API either.
        try {
            store.read(82L, "music_file")
            throw AssertionError("Copied plain attachment must stay plain")
        } catch (expected: IOException) { assertTrue(expected.message!!.contains("without encryption")) }
    }
}
