package com.nestnote.app

import java.io.IOException

private const val WAV_OUTPUT_SAMPLE_RATE = 16_000

/** Converts uncompressed 16-bit mono/stereo WAV to the inline player’s 16 kHz mono PCM format. */
internal fun decodePcmWav(bytes: ByteArray): ByteArray {
    if (bytes.size < 44 || String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF" ||
        String(bytes, 8, 4, Charsets.US_ASCII) != "WAVE"
    ) {
        throw IOException("Choose an uncompressed PCM WAV file (16-bit audio).")
    }
    var offset = 12
    var format = -1
    var channels = 0
    var sampleRate = 0
    var bits = 0
    var dataOffset = -1
    var dataLength = 0
    while (offset + 8 <= bytes.size) {
        val id = String(bytes, offset, 4, Charsets.US_ASCII)
        val length = java.nio.ByteBuffer.wrap(bytes, offset + 4, 4)
            .order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        if (length < 0 || offset + 8L + length > bytes.size) {
            throw IOException("The WAV file is malformed.")
        }
        if (id == "fmt " && length >= 16) {
            val b = java.nio.ByteBuffer.wrap(bytes, offset + 8, length)
                .order(java.nio.ByteOrder.LITTLE_ENDIAN)
            format = b.short.toInt() and 0xffff
            channels = b.short.toInt() and 0xffff
            sampleRate = b.int; b.int; b.short
            bits = b.short.toInt() and 0xffff
        }
        if (id == "data") { dataOffset = offset + 8; dataLength = length }
        offset += 8 + length + (length and 1)
    }
    if (format != 1 || channels !in 1..2 || bits != 16 || sampleRate <= 0 ||
        dataOffset < 0 || dataLength < channels * 2
    ) {
        throw IOException("Unsupported WAV format. Use uncompressed 16-bit mono or stereo WAV.")
    }
    val frames = dataLength / (channels * 2)
    val requestedOutputFrames = (frames.toLong() * WAV_OUTPUT_SAMPLE_RATE / sampleRate).coerceAtLeast(1L)
    val maxOutputFrames = com.nestnote.app.voicenote.VoiceNoteRepository.MAX_RECORDING_BYTES / 2
    if (requestedOutputFrames > maxOutputFrames) {
        throw IOException("Decoded audio exceeds the 25 MiB attachment limit.")
    }
    val outFrames = requestedOutputFrames.toInt()
    val input = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    val output = java.nio.ByteBuffer.allocate(outFrames * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    repeat(outFrames) { i ->
        val frame = ((i.toLong() * sampleRate) / WAV_OUTPUT_SAMPLE_RATE).toInt().coerceAtMost(frames - 1)
        val left = input.getShort(dataOffset + frame * channels * 2).toInt()
        val sample = if (channels == 2) {
            (left + input.getShort(dataOffset + frame * 4 + 2).toInt()) / 2
        } else left
        output.putShort(i * 2, sample.toShort())
    }
    return output.array()
}

