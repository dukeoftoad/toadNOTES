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
    private val onFinished: (String?) -> Unit
) {
    private val appContext = context.applicationContext
    private val running = AtomicBoolean(false)
    @Volatile private var audioTrack: AudioTrack? = null
    private var playbackThread: Thread? = null

    fun start() {
        if (!running.compareAndSet(false, true)) return
        playbackThread = Thread({
            var errorMessage: String? = null
            var track: AudioTrack? = null
            try {
                // read() returns data only after GCM authentication succeeds.
                val pcm = store.read(attachment.noteId, attachment.attachmentId)
                if (pcm.isEmpty()) throw IOException("This recording is empty.")
                val minBuffer = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
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
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .build()
                    )
                    .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
                if (track.state != AudioTrack.STATE_INITIALIZED) {
                    throw IOException("Could not initialize audio playback.")
                }
                audioTrack = track
                track.play()
                var offset = 0
                while (running.get() && offset < pcm.size) {
                    val count = track.write(pcm, offset, minOf(8192, pcm.size - offset))
                    if (count < 0) throw IOException("Audio playback failed ($count).")
                    if (count == 0) Thread.yield() else offset += count
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
                    track.release()
                }
                Handler(Looper.getMainLooper()).post { onFinished(errorMessage) }
            }
        }, "nestnote-voice-playback").apply { start() }
    }

    fun stop() {
        running.set(false)
        runCatching { audioTrack?.pause() }
        runCatching { audioTrack?.flush() }
        runCatching { audioTrack?.stop() }
    }

    companion object {
        private const val SAMPLE_RATE = 16_000
    }
}
