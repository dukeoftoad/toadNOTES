package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.ByteArrayInputStream
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

/**
 * Regression coverage for the record -> review -> attach/discard lifecycle.
 *
 * A reviewed clip may exist as encrypted staged bytes, but it must not appear in the
 * user-visible attachment manifest until the explicit Attach action commits metadata.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceRecordingLifecycleRegressionTest {
    private fun newKey() = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()

    @Test
    fun stoppingIntoReviewDoesNotPublishAttachmentUntilExplicitAttach() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-review-explicit-attach").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val noteId = 810L
        val clipBytes = byteArrayOf(10, 20, 30, 40, 50)

        // Stop finalizes encrypted bytes for preview; no manifest entry means no attachment card.
        val pending = store.save(noteId, "pending_review_clip", ByteArrayInputStream(clipBytes))
        assertTrue(store.list(noteId).any { it.attachmentId == pending.attachmentId })
        assertTrue("Pending review must not be published as an attachment", manifest.list(noteId, store).isEmpty())
        assertArrayEquals(clipBytes, store.read(noteId, pending.attachmentId))

        // Explicit Attach commits the manifest entry and the user-edited display name.
        manifest.put(
            VoiceAttachmentMetadata(
                noteId = noteId,
                attachmentId = pending.attachmentId,
                createdAtMillis = 100L,
                durationMillis = 250L,
                encryptedSizeBytes = pending.encryptedSizeBytes,
                displayName = "Interview excerpt"
            ),
            store
        )

        val attached = manifest.list(noteId, store)
        assertEquals(1, attached.size)
        assertEquals("Interview excerpt", attached.single().displayName)
        assertEquals(pending.attachmentId, attached.single().attachmentId)
        assertArrayEquals(clipBytes, store.read(noteId, pending.attachmentId))
    }

    @Test
    fun discardingPendingReviewPreservesExistingAttachmentsAndNoteTextState() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-review-discard-preserves-existing").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val noteId = 811L
        val originalBytes = byteArrayOf(1, 3, 5, 7)
        val pendingBytes = byteArrayOf(2, 4, 6, 8)

        val original = store.save(noteId, "existing_attached_clip", ByteArrayInputStream(originalBytes))
        val originalMetadata = VoiceAttachmentMetadata(
            noteId = noteId,
            attachmentId = original.attachmentId,
            createdAtMillis = 1L,
            durationMillis = 100L,
            encryptedSizeBytes = original.encryptedSizeBytes,
            displayName = "Existing clip"
        )
        manifest.put(originalMetadata, store)

        // A reviewed recording is staged independently and is not added to the manifest.
        val pending = store.save(noteId, "discarded_pending_clip", ByteArrayInputStream(pendingBytes))
        assertEquals(listOf("existing_attached_clip"), manifest.list(noteId, store).map { it.attachmentId })

        // Mirrors Discard review: delete only the pending clip, never clear the note's attachment set.
        assertTrue(store.delete(noteId, pending.attachmentId))

        assertFalse(store.list(noteId).any { it.attachmentId == pending.attachmentId })
        assertArrayEquals(originalBytes, store.read(noteId, original.attachmentId))
        assertEquals(listOf(originalMetadata), manifest.list(noteId, store))
        assertTrue("Original attachment remains encrypted on disk",
            java.io.File(java.io.File(root, noteId.toString()), "existing_attached_clip.nva").isFile)
    }

    @Test
    fun failedAttachMetadataCommitLeavesPendingPayloadRecoverableForRetry() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-review-attach-retry").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val noteId = 812L
        val pendingBytes = byteArrayOf(11, 22, 33)

        val pending = store.save(noteId, "retry_pending_clip", ByteArrayInputStream(pendingBytes))
        try {
            manifest.put(
                VoiceAttachmentMetadata(
                    noteId = noteId,
                    attachmentId = pending.attachmentId,
                    createdAtMillis = 2L,
                    durationMillis = 10L,
                    encryptedSizeBytes = pending.encryptedSizeBytes + 1L,
                    displayName = "Retry clip"
                ),
                store
            )
            throw AssertionError("Mismatched metadata must not be committed")
        } catch (expected: java.io.IOException) {
            assertTrue(expected.message!!.contains("size does not match"))
        }

        assertTrue(manifest.list(noteId, store).isEmpty())
        assertArrayEquals(pendingBytes, store.read(noteId, pending.attachmentId))
        manifest.put(
            VoiceAttachmentMetadata(
                noteId = noteId,
                attachmentId = pending.attachmentId,
                createdAtMillis = 2L,
                durationMillis = 10L,
                encryptedSizeBytes = pending.encryptedSizeBytes,
                displayName = "Retry clip"
            ),
            store
        )
        assertEquals("Retry clip", manifest.list(noteId, store).single().displayName)
    }

    @Test
    fun multipleAttachedClipsPersistIndependentlyWithTheirOwnMetadata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val root = Files.createTempDirectory("nestnote-multiple-attached-clips").toFile()
        val key = newKey()
        val store = VoiceAttachmentStore(context, { key }, root)
        val manifest = VoiceAttachmentManifest(context, root)
        val noteId = 813L
        val firstBytes = byteArrayOf(1, 2, 3, 4)
        val secondBytes = byteArrayOf(9, 8, 7, 6, 5)

        val first = store.save(noteId, "first_clip", ByteArrayInputStream(firstBytes))
        manifest.put(
            VoiceAttachmentMetadata(noteId, first.attachmentId, 10L, 100L, first.encryptedSizeBytes, "First clip"),
            store
        )
        val second = store.save(noteId, "second_clip", ByteArrayInputStream(secondBytes))
        manifest.put(
            VoiceAttachmentMetadata(noteId, second.attachmentId, 20L, 200L, second.encryptedSizeBytes, "Second clip"),
            store
        )

        val attached = manifest.list(noteId, store)
        assertEquals(listOf("First clip", "Second clip"), attached.map { it.displayName })
        assertEquals(2, attached.map { it.attachmentId }.distinct().size)
        assertArrayEquals(firstBytes, store.read(noteId, first.attachmentId))
        assertArrayEquals(secondBytes, store.read(noteId, second.attachmentId))

        // Re-open the store and manifest from the same backing directory to verify persistence.
        val reopenedStore = VoiceAttachmentStore(context, { key }, root)
        val reopenedManifest = VoiceAttachmentManifest(context, root)
        assertEquals(listOf("First clip", "Second clip"), reopenedManifest.list(noteId, reopenedStore).map { it.displayName })
        assertArrayEquals(firstBytes, reopenedStore.read(noteId, first.attachmentId))
        assertArrayEquals(secondBytes, reopenedStore.read(noteId, second.attachmentId))
    }
}
