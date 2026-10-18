package com.nestnote.app

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Plays a committed encrypted PCM attachment only after VoiceAttachmentStore has authenticated
 * and decrypted the complete GCM payload. Plaintext exists only in memory, never in a file.
 */
internal class PcmVoicePlayback(
    context: Context,
    private val store: VoiceAttachmentStore,
    private val attachment: VoiceAttachment,
    private val onProgress: (Long, Long) -> Unit = { _, _ -> },
    private val onFinished: (String?) -> Unit
) {
    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var playbackThread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        playbackThread = Thread({
            var errorMessage: String? = null
            var track: AudioTrack? = null
            try {
                // read() returns only after AES-GCM authentication succeeds. Do not construct,
                // start, or write to AudioTrack until the entire payload has authenticated.
                val pcm = store.read(attachment.noteId, attachment.attachmentId)
                PcmPlaybackFormat.validateAuthenticatedPcm(pcm)

                val minBuffer = AudioTrack.getMinBufferSize(
                    PcmPlaybackFormat.SAMPLE_RATE_HZ,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                if (minBuffer <= 0) throw IOException("Audio playback is unavailable on this device.")
                track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(PcmPlaybackFormat.SAMPLE_RATE_HZ)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuffer, PcmPlaybackFormat.WRITE_CHUNK_BYTES))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    throw IOException("Could not initialize audio playback.")
                }
                audioTrack = track
                if (running.get()) track.play()
                var offset = 0
                var zeroWrites = 0
                var lastProgressBytes = 0
                while (running.get() && offset < pcm.size) {
                    val count = track.write(
                        pcm, offset, minOf(PcmPlaybackFormat.WRITE_CHUNK_BYTES, pcm.size - offset)
                    )
                    when {
                        count < 0 -> throw IOException("Audio playback failed ($count).")
                        count == 0 -> {
                            zeroWrites++
                            if (zeroWrites >= MAX_CONSECUTIVE_ZERO_WRITES) {
                                throw IOException("Audio playback stopped accepting audio data.")
                            }
                            Thread.yield()
                        }
                        else -> {
                            offset += count
                            zeroWrites = 0
                            if (offset == pcm.size || offset - lastProgressBytes >= 32_000) {
                                lastProgressBytes = offset
                                val positionMs = offset * 1000L / (PcmPlaybackFormat.SAMPLE_RATE_HZ * PcmPlaybackFormat.BYTES_PER_SAMPLE)
                                val durationMs = pcm.size * 1000L / (PcmPlaybackFormat.SAMPLE_RATE_HZ * PcmPlaybackFormat.BYTES_PER_SAMPLE)
                                Handler(Looper.getMainLooper()).post { onProgress(positionMs, durationMs) }
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                if (running.get()) errorMessage = error.message ?: "Couldn't play this recording."
            } finally {
                running.set(false)
                audioTrack = null
                if (track != null) {
                    runCatching { track.pause() }
                    runCatching { track.flush() }
                    runCatching { track.stop() }
                    runCatching { track.release() }
                }
                Handler(Looper.getMainLooper()).post { onFinished(errorMessage) }
            }
        }, "nestnote-voice-playback").apply { start() }
    }

    fun stop() {
        running.set(false)
        val active = audioTrack
        if (active != null) {
            runCatching { active.pause() }
            runCatching { active.flush() }
            runCatching { active.stop() }
        }
    }

    companion object {
        private const val MAX_CONSECUTIVE_ZERO_WRITES = 100
    }
}
