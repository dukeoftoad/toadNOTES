package com.nestnote.app

import java.io.IOException

/**
 * Fixed PCM contract for voice attachments. Recording and playback must stay aligned:
 * signed 16-bit samples, one channel, 16 kHz, little-endian PCM as produced by AudioRecord.
 */
internal object PcmPlaybackFormat {
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNEL_COUNT = 1
    const val BYTES_PER_SAMPLE = 2
    const val WRITE_CHUNK_BYTES = 8 * 1024

    init {
        check(WRITE_CHUNK_BYTES % BYTES_PER_SAMPLE == 0)
    }

    /** Call only after the encrypted store has authenticated and decrypted the complete payload. */
    @Throws(IOException::class)
    fun validateAuthenticatedPcm(pcm: ByteArray) {
        if (pcm.isEmpty()) throw IOException("This recording is empty.")
        if (pcm.size % BYTES_PER_SAMPLE != 0) {
            throw IOException("This recording contains an incomplete PCM16 sample.")
        }
    }
}
