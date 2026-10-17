package com.nestnote.app

import java.io.IOException
import java.io.OutputStream

/** Small adapter boundary for microphone capture; JVM tests can provide deterministic fakes. */
internal interface PcmCapture {
    fun start()
    fun read(buffer: ByteArray): Int
    fun pause()
    fun resume()
    fun stop()
    fun release()
}

internal class PcmCaptureSizeLimitException : IOException("Recording reached the configured size limit")

/**
 * Pumps PCM into the encrypted store's pipe. Capture implementations return -1 for clean EOF;
 * hardware adapters translate platform-specific negative error codes into IOException.
 */
internal object PcmCapturePump {
    fun capture(
        capture: PcmCapture,
        output: OutputStream,
        maxBytes: Long,
        shouldContinue: () -> Boolean,
        isPaused: () -> Boolean,
        onBytesCaptured: (Long) -> Unit,
        onCaptureFailure: (Exception) -> Unit = {}
    ): Long {
        require(maxBytes >= 0) { "maxBytes must not be negative" }
        var total = 0L
        val buffer = ByteArray(4096)
        var started = false
        try {
            while (shouldContinue()) {
                if (isPaused()) {
                    if (started) {
                        capture.pause()
                        started = false
                    }
                    Thread.yield()
                    continue
                }
                if (!started) {
                    if (total == 0L) capture.start() else capture.resume()
                    started = true
                }
                val count = capture.read(buffer)
                if (count == -1) break
                if (count < -1) throw IOException("Invalid PCM capture result: $count")
                if (count == 0) continue
                if (count > buffer.size) throw IOException("PCM capture returned more bytes than requested")
                if (total + count > maxBytes) throw PcmCaptureSizeLimitException()
                output.write(buffer, 0, count)
                total += count
                onBytesCaptured(total)
            }
            return total
        } catch (error: Exception) {
            // Record the failure before closing the pipe so storage cannot commit partial PCM.
            onCaptureFailure(error)
            throw error
        } finally {
            runCatching { capture.stop() }
            runCatching { capture.release() }
            runCatching { output.close() }
        }
    }
}

/** AudioRecord-backed hardware adapter; all Android-specific calls stay here. */
internal class AudioRecordPcmCapture(private val record: android.media.AudioRecord) : PcmCapture {
    @Volatile private var started = false

    override fun start() {
        record.startRecording()
        started = true
    }

    override fun read(buffer: ByteArray): Int {
        val count = record.read(buffer, 0, buffer.size)
        if (count < 0) throw IOException("Microphone read failed ($count)")
        return count
    }

    override fun pause() {
        if (started) {
            runCatching { record.stop() }
            started = false
        }
    }

    override fun resume() {
        if (!started) start()
    }

    override fun stop() {
        if (started) runCatching { record.stop() }
        started = false
    }

    override fun release() {
        record.release()
    }
}
