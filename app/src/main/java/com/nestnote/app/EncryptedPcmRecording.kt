package com.nestnote.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal const val VOICE_SAMPLE_RATE = 16_000
private const val VOICE_CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
private const val VOICE_ENCODING = AudioFormat.ENCODING_PCM_16BIT

internal class EncryptedPcmRecording(
    private val context: Context,
    private val repository: com.nestnote.app.voicenote.VoiceNoteRepository,
    private val noteId: Long,
    private val attachmentId: String,
    private val onFinished: (Result<VoiceAttachment>) -> Unit
) {
    private val lifecycle = VoiceRecordingLifecycle()
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val discarded = AtomicBoolean(false)
    val isDiscarded: Boolean get() = discarded.get()
    private val audioBytes = AtomicLong(0)
    private val limitExceeded = AtomicBoolean(false)
    private val recordingFailure = AtomicReference<IOException?>(null)
    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var captureAdapter: AudioRecordPcmCapture? = null
    @Volatile private var pipeOutput: PipedOutputStream? = null
    private var recorderThread: Thread? = null
    private var storageThread: Thread? = null

    fun start() {
        // Permission may be revoked after the UI's permission callback and before this
        // worker starts. Check immediately before creating AudioRecord as well.
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            throw IOException("Microphone permission is required to record voice notes.")
        }
        val minBuffer = AudioRecord.getMinBufferSize(VOICE_SAMPLE_RATE, VOICE_CHANNEL_IN, VOICE_ENCODING)
        if (minBuffer <= 0) throw IOException("Microphone recording is unavailable on this device")
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.MIC, VOICE_SAMPLE_RATE, VOICE_CHANNEL_IN,
                VOICE_ENCODING, maxOf(minBuffer * 2, 8192)
            )
        } catch (error: SecurityException) {
            throw IOException("Microphone permission is required to record voice notes.", error)
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IOException("Could not initialize the microphone")
        }
        val input = PipedInputStream(64 * 1024)
        val output = PipedOutputStream(input)
        if (!lifecycle.start()) {
            record.release()
            input.close()
            output.close()
            throw IOException("This recording session has already been started or closed")
        }
        audioRecord = record
        pipeOutput = output
        running.set(true)

        storageThread = Thread({
            val result = runCatching {
                val saved = repository.saveForReview(noteId, attachmentId, input)
                val failure = recordingFailure.get()
                if (failure != null || audioBytes.get() == 0L || limitExceeded.get()) {
                    repository.deletePayload(noteId, attachmentId)
                    throw failure ?: IOException(
                        if (limitExceeded.get()) "Recording reached the 25 MiB limit and was discarded."
                        else "No audio was captured. Try recording again."
                    )
                }
                saved
            }
            runCatching { input.close() }
            Handler(Looper.getMainLooper()).post { onFinished(result) }
        }, "nestnote-voice-encrypt").apply { start() }

        val capture = AudioRecordPcmCapture(record)
        captureAdapter = capture
        recorderThread = Thread({
            try {
                val captured = PcmCapturePump.capture(
                    capture = capture,
                    output = output,
                    maxBytes = com.nestnote.app.voicenote.VoiceNoteRepository.MAX_RECORDING_BYTES.toLong(),
                    shouldContinue = { running.get() },
                    isPaused = { paused.get() },
                    onBytesCaptured = { audioBytes.set(it) },
                    onCaptureFailure = { error ->
                        lifecycle.fail()
                        if (error is PcmCaptureSizeLimitException) {
                            limitExceeded.set(true)
                        } else if (running.get()) {
                            recordingFailure.compareAndSet(
                                null, IOException("Recording failed: " + (error.message ?: "unknown error"), error)
                            )
                        }
                        running.set(false)
                    }
                )
                if (captured > 0L) audioBytes.set(captured)
            } catch (error: PcmCaptureSizeLimitException) {
                lifecycle.fail()
                limitExceeded.set(true)
                running.set(false)
            } catch (error: Exception) {
                if (running.get() && !limitExceeded.get()) {
                    lifecycle.fail()
                    recordingFailure.compareAndSet(null, IOException("Recording failed: ${error.message ?: "unknown error"}", error))
                    running.set(false)
                }
            } finally {
                captureAdapter = null
                audioRecord = null
            }
        }, "nestnote-voice-record").apply { start() }
    }

    @Synchronized fun pause() {
        if (lifecycle.pause()) paused.set(true)
    }

    @Synchronized fun resume() {
        if (lifecycle.resume()) paused.set(false)
    }

    @Synchronized fun stop() {
        lifecycle.stop()
        running.set(false)
        paused.set(false)
        runCatching { captureAdapter?.stop() }
        runCatching { audioRecord?.stop() }
        runCatching { pipeOutput?.close() }
    }

    /**
     * Prevents a queued main-thread completion callback from recreating metadata after discard.
     * The caller must remove the attachment from its session set before waiting for workers.
     */
    @Synchronized fun discardAndWait() {
        lifecycle.discard()
        discarded.set(true)
        stopAndWait()
    }

    fun stopAndWait() {
        stop()
        var interrupted = false
        listOfNotNull(recorderThread, storageThread).forEach { thread ->
            while (thread.isAlive) {
                try {
                    thread.join()
                } catch (_: InterruptedException) {
                    interrupted = true
                }
            }
        }
        if (interrupted) Thread.currentThread().interrupt()
    }
}

