package com.nestnote.app

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class PcmWavDecoderTest {
    @Test
    fun decodes16BitMonoPcmWithoutChangingSamplesAtTargetRate() {
        val decoded = decodePcmWav(wav(channels = 1, sampleRate = 16_000, samples = shortArrayOf(1200, -900)))

        assertArrayEquals(byteArrayOf(0xB0.toByte(), 0x04, 0x7C.toByte(), 0xFC.toByte()), decoded)
    }

    @Test
    fun averagesStereoChannelsToMono() {
        val decoded = decodePcmWav(
            wav(channels = 2, sampleRate = 16_000, samples = shortArrayOf(1000, 3000, -1000, 1000))
        )
        val samples = ByteBuffer.wrap(decoded).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(2000, samples.short.toInt())
        assertEquals(0, samples.short.toInt())
    }

    @Test(expected = IOException::class)
    fun rejectsNonWavInput() {
        decodePcmWav(byteArrayOf(1, 2, 3, 4))
    }

    @Test(expected = IOException::class)
    fun rejectsWavWhoseDecodedPcmWouldExceedAttachmentLimit() {
        // A tiny source file with a pathological sample rate must not trigger a huge allocation.
        decodePcmWav(wav(channels = 1, sampleRate = 1, samples = ShortArray(2_000)))
    }

    private fun wav(channels: Int, sampleRate: Int, samples: ShortArray): ByteArray {
        val dataLength = samples.size * 2
        val buffer = ByteBuffer.allocate(44 + dataLength).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
        buffer.putInt(36 + dataLength)
        buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
        buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(channels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(sampleRate * channels * 2)
        buffer.putShort((channels * 2).toShort())
        buffer.putShort(16)
        buffer.put("data".toByteArray(Charsets.US_ASCII))
        buffer.putInt(dataLength)
        samples.forEach { buffer.putShort(it) }
        return buffer.array()
    }
}
