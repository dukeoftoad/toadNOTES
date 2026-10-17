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

}
