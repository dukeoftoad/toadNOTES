package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.crypto.KeyGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceAttachmentManifestTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun metadataRoundTripsOnlyAfterEncryptedPayloadIsCommitted() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-manifest").toFile()
        val store = VoiceAttachmentStore(context, { newKey() }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val attachment = store.save(41L, "recording_1", ByteArrayInputStream(ByteArray(3_200)))

        val expected = VoiceAttachmentMetadata(
            noteId = 41L,
            attachmentId = "recording_1",
            createdAtMillis = 1234L,
            durationMillis = 100L,
            encryptedSizeBytes = attachment.encryptedSizeBytes,
            displayName = "Voice recording"
        )
        manifest.put(expected, store)

        assertEquals(listOf(expected), manifest.list(41L, store))
    }

    @Test
    fun reconciliationRemovesStaleIndexButPreservesEncryptedAudio() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-reconcile").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val attachment = store.save(42L, "keep_audio", ByteArrayInputStream(byteArrayOf(1, 2, 3)))
        manifest.put(
            VoiceAttachmentMetadata(
                noteId = 42L,
                attachmentId = "keep_audio",
                createdAtMillis = 100L,
                durationMillis = 1L,
                encryptedSizeBytes = attachment.encryptedSizeBytes,
                displayName = "Voice recording"
            ),
            store
        )

        assertEquals(1, manifest.reconcile(store, emptySet()))
        assertTrue(File(File(root, "42"), "keep_audio.nva").isFile)
        assertTrue(manifest.list(42L, store).isEmpty())
    }
    @Test
    fun missingManifestNeverDeletesValidEncryptedPayload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-missing-manifest").toFile()
        val store = VoiceAttachmentStore(context, { newKey() }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        store.save(43L, "keep_missing_index", ByteArrayInputStream(byteArrayOf(4, 5, 6)))

        assertEquals(0, manifest.reconcile(store, setOf(43L)))
        assertTrue(File(File(root, "43"), "keep_missing_index.nva").isFile)
        assertTrue(manifest.list(43L, store).isEmpty())
    }

    @Test
    fun corruptManifestFailsClosedAndPreservesEncryptedPayload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-corrupt-manifest").toFile()
        val store = VoiceAttachmentStore(context, { newKey() }, root)
        store.save(44L, "keep_corrupt_index", ByteArrayInputStream(byteArrayOf(7, 8, 9)))
        File(root, "manifest_v1.json").writeText("{ definitely not valid JSON")

        try {
            VoiceAttachmentManifest(context, root).reconcile(store, setOf(44L))
            throw AssertionError("Corrupt manifest must fail closed")
        } catch (expected: java.io.IOException) {
            assertTrue(expected.message!!.contains("metadata is unreadable"))
        }
        assertTrue(File(File(root, "44"), "keep_corrupt_index.nva").isFile)
    }

    @Test
    fun oversizedManifestIsRejectedBeforeParsingAndPreservesPayload() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-voice-oversized-manifest").toFile()
        val store = VoiceAttachmentStore(context, { newKey() }, root)
        store.save(45L, "keep_large_index", ByteArrayInputStream(byteArrayOf(10, 11, 12)))
        File(root, "manifest_v1.json").writeText(" ".repeat(4 * 1024 * 1024 + 1))

        try {
            VoiceAttachmentManifest(context, root).reconcile(store, setOf(45L))
            throw AssertionError("Oversized manifest must fail closed")
        } catch (expected: java.io.IOException) {
            assertTrue(expected.message!!.contains("metadata is unreadable"))
        }
        assertTrue(File(File(root, "45"), "keep_large_index.nva").isFile)
    }


    @Test
    fun discardingNewDraftRemovesOnlyItsStagedAttachments() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-draft-discard").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val savedNoteId = 700L
        val draftId = 701L

        val savedPayload = byteArrayOf(9, 8, 7, 6)
        val saved = store.save(savedNoteId, "saved_recording", ByteArrayInputStream(savedPayload))
        val savedMetadata = VoiceAttachmentMetadata(
            noteId = savedNoteId,
            attachmentId = "saved_recording",
            createdAtMillis = 1L,
            durationMillis = 1L,
            encryptedSizeBytes = saved.encryptedSizeBytes,
            displayName = "Saved recording"
        )
        manifest.put(savedMetadata, store)

        val stagedPayload = byteArrayOf(1, 2, 3, 4)
        val staged = store.save(draftId, "new_draft_recording", ByteArrayInputStream(stagedPayload))
        manifest.put(
            VoiceAttachmentMetadata(
                noteId = draftId,
                attachmentId = "new_draft_recording",
                createdAtMillis = 2L,
                durationMillis = 1L,
                encryptedSizeBytes = staged.encryptedSizeBytes,
                displayName = "Draft recording"
            ),
            store
        )

        assertTrue(store.deleteAll(draftId))
        manifest.removeNote(draftId)

        assertTrue(store.list(draftId).isEmpty())
        assertTrue(manifest.list(draftId, store).isEmpty())
        assertEquals(savedPayload.toList(), store.read(savedNoteId, "saved_recording").toList())
        assertEquals(listOf(savedMetadata), manifest.list(savedNoteId, store))
    }

    @Test
    fun discardingEditorSessionRemovesOnlyNewlyStagedAttachmentForSavedNote() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-existing-note-discard").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val noteId = 702L

        fun saveAndIndex(id: String, bytes: ByteArray, created: Long) {
            val saved = store.save(noteId, id, ByteArrayInputStream(bytes))
            manifest.put(
                VoiceAttachmentMetadata(
                    noteId = noteId,
                    attachmentId = id,
                    createdAtMillis = created,
                    durationMillis = 1L,
                    encryptedSizeBytes = saved.encryptedSizeBytes,
                    displayName = id
                ),
                store
            )
        }

        val originalBytes = byteArrayOf(4, 3, 2, 1)
        val newlyStagedBytes = byteArrayOf(5, 6, 7, 8)
        saveAndIndex("original_recording", originalBytes, 1L)
        saveAndIndex("staged_recording", newlyStagedBytes, 2L)

        // Mirrors the editor's session-discard handler: remove only identities created this session.
        assertTrue(store.delete(noteId, "staged_recording"))
        manifest.remove(noteId, "staged_recording")

        assertEquals(originalBytes.toList(), store.read(noteId, "original_recording").toList())
        assertEquals(listOf("original_recording"), manifest.list(noteId, store).map { it.attachmentId })
        assertTrue(store.list(noteId).none { it.attachmentId == "staged_recording" })
    }

    @Test
    fun attachmentStagedBeforeFirstSaveRemainsBoundToReservedDraftId() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-draft-identity").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val existing = listOf(NestNote(id = 500L, title = "Existing", body = ""))
        val draftId = NoteStore.allocateNoteId(existing, nowMillis = 500L)
        assertEquals(501L, draftId)

        // The editor can stage audio before its note is persisted, using its reserved identity.
        val pcm = byteArrayOf(1, 2, 3, 4)
        val attachment = store.save(draftId, "draft_recording", ByteArrayInputStream(pcm))
        val metadata = VoiceAttachmentMetadata(
            noteId = draftId,
            attachmentId = "draft_recording",
            createdAtMillis = 1234L,
            durationMillis = 1L,
            encryptedSizeBytes = attachment.encryptedSizeBytes,
            displayName = "Voice recording"
        )
        manifest.put(metadata, store)
        assertTrue(manifest.list(draftId, store).isNotEmpty())

        // Saving the first note uses the exact reserved ID; the authenticated payload and
        // metadata therefore remain discoverable under the newly persisted note.
        val savedDraft = NestNote(id = draftId, title = "Draft with audio", body = "Text")
        assertEquals(draftId, savedDraft.id)
        assertEquals(pcm.toList(), store.read(savedDraft.id, "draft_recording").toList())
        assertEquals(listOf(metadata), manifest.list(savedDraft.id, store))
    }

}
