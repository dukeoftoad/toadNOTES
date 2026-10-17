package com.nestnote.app

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PcmPlaybackFormatTest {
    @Test
    fun playbackContractMatchesRecorderPcmFormat() {
        assertEquals(16_000, PcmPlaybackFormat.SAMPLE_RATE_HZ)
        assertEquals(1, PcmPlaybackFormat.CHANNEL_COUNT)
        assertEquals(2, PcmPlaybackFormat.BYTES_PER_SAMPLE)
        assertEquals(0, PcmPlaybackFormat.WRITE_CHUNK_BYTES % PcmPlaybackFormat.BYTES_PER_SAMPLE)
    }

    @Test
    fun acceptsCompletePcm16FramesOfDifferentLengths() {
        PcmPlaybackFormat.validateAuthenticatedPcm(byteArrayOf(0, 0))
        PcmPlaybackFormat.validateAuthenticatedPcm(ByteArray(32_000))
        PcmPlaybackFormat.validateAuthenticatedPcm(ByteArray(PcmPlaybackFormat.WRITE_CHUNK_BYTES + 2))
    }

    @Test
    fun rejectsEmptyAndTruncatedPcm16SampleBeforeAudioTrackSetup() {
        val empty = runCatching { PcmPlaybackFormat.validateAuthenticatedPcm(byteArrayOf()) }.exceptionOrNull()
        assertTrue(empty is IOException)
        assertEquals("This recording is empty.", empty?.message)

        val oddLength = runCatching { PcmPlaybackFormat.validateAuthenticatedPcm(byteArrayOf(0, 1, 2)) }.exceptionOrNull()
        assertTrue(oddLength is IOException)
        assertEquals("This recording contains an incomplete PCM16 sample.", oddLength?.message)
    }

    @Test
    fun writeChunkSizeIsBoundedAndSampleAligned() {
        assertTrue(PcmPlaybackFormat.WRITE_CHUNK_BYTES > 0)
        assertTrue(PcmPlaybackFormat.WRITE_CHUNK_BYTES <= 16 * 1024)
        assertFalse(PcmPlaybackFormat.WRITE_CHUNK_BYTES % 2 != 0)
    }
}
