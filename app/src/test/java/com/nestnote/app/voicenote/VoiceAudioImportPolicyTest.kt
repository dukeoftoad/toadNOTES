package com.nestnote.app.voicenote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class VoiceAudioImportPolicyTest {
    @Test
    fun acceptsCommonAudioExtensionsCaseInsensitively() {
        listOf("call.MP3", "clip.opus", "memo.AMR", "recording.m4a", "voice.3gp", "clip.wav")
            .forEach { name ->
                assertTrue("$name should be accepted", VoiceAudioImportPolicy.isAudioDocument(name, null))
            }
    }

    @Test
    fun acceptsAudioMimeTypeEvenWhenProviderUsesUnknownExtension() {
        assertTrue(VoiceAudioImportPolicy.isAudioDocument("recording.bin", "audio/mpeg"))
    }

    @Test
    fun rejectsNonAudioDocumentsWithoutAudioMimeType() {
        assertFalse(VoiceAudioImportPolicy.isAudioDocument("notes.pdf", "application/pdf"))
        assertFalse(VoiceAudioImportPolicy.isAudioDocument("unknown", null))
    }

    @Test
    fun infersCommonExtensionWhenProviderOmitsOne() {
        assertEquals("Imported audio.mp3", VoiceAudioImportPolicy.ensureAudioExtension("Imported audio", "audio/mpeg"))
        assertEquals("Imported audio.wav", VoiceAudioImportPolicy.ensureAudioExtension("Imported audio", "audio/wav"))
    }

    @Test
    fun usesRecognizableFallbackMarkerForUnknownAudioMimeType() {
        assertEquals("Imported audio.audio", VoiceAudioImportPolicy.ensureAudioExtension("Imported audio", "audio/x-custom"))
    }

    @Test
    fun preservesExistingFilenameAndExtension() {
        assertEquals("call-recording.opus", VoiceAudioImportPolicy.ensureAudioExtension("call-recording.opus", "audio/ogg"))
    }

    @Test
    fun filesAtOrAbove30MiBAreStoredPlainWithoutEncryption() {
        assertTrue(VoiceAudioImportPolicy.storesPlain(30L * 1024 * 1024))
        assertTrue(VoiceAudioImportPolicy.storesPlain(40L * 1024 * 1024))
        assertTrue(VoiceAudioImportPolicy.storesPlain(Long.MAX_VALUE))
    }

    @Test
    fun filesBelow30MiBKeepEndToEndEncryption() {
        assertFalse(VoiceAudioImportPolicy.storesPlain(0L))
        assertFalse(VoiceAudioImportPolicy.storesPlain(29L * 1024 * 1024))
        assertFalse(VoiceAudioImportPolicy.storesPlain(30L * 1024 * 1024 - 1))
    }

    @Test
    fun unknownDeclaredSizeStaysEncrypted() {
        assertFalse(VoiceAudioImportPolicy.storesPlain(null))
    }

    @Test
    fun plainStorageThresholdIs30MiB() {
        assertEquals(30 * 1024 * 1024, VoiceNoteRepository.PLAIN_STORAGE_MIN_BYTES)
    }
}
