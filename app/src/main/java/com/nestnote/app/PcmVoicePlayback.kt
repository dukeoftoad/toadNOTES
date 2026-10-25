package com.nestnote.app

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Plays an encrypted PCM attachment with the platform [MediaPlayer] (built-in seek, pause and
 * completion). The store authenticates and decrypts the payload; a WAV header is added in memory,
 * so plaintext never touches disk. Callbacks run on the main thread.
 *
 * [onFinished] fires only on natural completion (null) or error (message), never after [stop].
 *
 * This class is single-use: create one instance per playback session. Calling [start] again after
 * [stop] will not restart playback.
 */
internal class PcmVoicePlayback(
    @Suppress("UNUSED_PARAMETER") context: Context,
    private val readAttachment: (noteId: Long, attachmentId: String) -> ByteArray,
    private val attachment: VoiceAttachment,
    private val startAtMs: Long = 0L,
    private val playbackSpeed: Float = 1f,
    private val onProgress: (Long, Long) -> Unit = { _, _ -> },
    private val onFinished: (String?) -> Unit
) {
    private companion object {
        /** Seek-bar / timer refresh cadence, in milliseconds. */
        const val PROGRESS_TICK_MS = 100L
    }

    private val main = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private var stopped = false
    private var paused = false

    private val ticker = object : Runnable {
        override fun run() {
            val p = player ?: return
            if (!stopped) {
                runCatching {
                    // MediaPlayer.duration can be -1 while preparing; report 0 so callers
                    // never have to guard a negative duration.
                    onProgress(p.currentPosition.toLong(), p.duration.coerceAtLeast(0).toLong())
                }
                main.postDelayed(this, PROGRESS_TICK_MS)
            }
        }
    }

    /** Decrypts off the main thread, then prepares and starts playback. */
    fun start() {
        // Allow a fresh start on a reused instance; stop() set this to true previously.
        stopped = false
        Thread({
            val result = runCatching {
                val pcm = readAttachment(attachment.noteId, attachment.attachmentId)
                PcmPlaybackFormat.validateAuthenticatedPcm(pcm)
                pcm
            }

            main.post {
                if (stopped) return@post
                result.onSuccess { begin(it) }
                    .onFailure { fail(it.message) }
            }
        }, "nestnote-voice-decrypt").start()
    }

    private fun begin(pcm: ByteArray) {
        val header = wavHeader(pcm.size)
        val mp = MediaPlayer()
        player = mp

        try {
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )

            mp.setDataSource(object : MediaDataSource() {
                private val headerSize = header.size.toLong()
                private val totalSize = headerSize + pcm.size.toLong()

                override fun getSize(): Long = totalSize

                override fun readAt(
                    position: Long,
                    buffer: ByteArray,
                    offset: Int,
                    size: Int
                ): Int {
                    // Validate every bound the MediaDataSource contract requires, so a bad
                    // request can never throw out of a native callback.
                    if (position < 0L || position >= totalSize) return -1
                    if (size < 0) return -1
                    if (size == 0) return 0
                    if (offset < 0 || offset > buffer.size) return -1

                    val maxWritable = buffer.size - offset
                    if (maxWritable <= 0) return 0

                    val toRead = minOf(
                        size.toLong(),
                        totalSize - position,
                        maxWritable.toLong()
                    ).toInt()
                    if (toRead <= 0) return 0

                    var pos = position
                    var destinationOffset = offset
                    var remaining = toRead

                    // Fast path: request starts inside the PCM payload.
                    if (pos >= headerSize) {
                        val pcmOffset = (pos - headerSize).toInt()
                        System.arraycopy(pcm, pcmOffset, buffer, destinationOffset, remaining)
                        return toRead
                    }

                    // Copy the portion of the request that belongs to the WAV header.
                    val headerOffset = pos.toInt()
                    val headerCount = minOf(remaining, header.size - headerOffset)
                    System.arraycopy(
                        header,
                        headerOffset,
                        buffer,
                        destinationOffset,
                        headerCount
                    )
                    pos += headerCount.toLong()
                    destinationOffset += headerCount
                    remaining -= headerCount

                    // Copy whatever remains from the PCM payload.
                    if (remaining > 0) {
                        val pcmOffset = (pos - headerSize).toInt()
                        System.arraycopy(pcm, pcmOffset, buffer, destinationOffset, remaining)
                    }

                    return toRead
                }

                override fun close() = Unit
            })

            mp.setOnPreparedListener { preparedPlayer ->
                if (stopped) return@setOnPreparedListener

                val safeSpeed = (playbackSpeed.takeIf { it.isFinite() } ?: 1f).coerceIn(0.5f, 2f)
                if (safeSpeed != 1f) {
                    preparedPlayer.playbackParams = preparedPlayer.playbackParams.setSpeed(safeSpeed)
                }

                if (startAtMs > 0L) {
                    preparedPlayer.seekTo(
                        startAtMs.coerceAtMost(Int.MAX_VALUE.toLong()),
                        MediaPlayer.SEEK_CLOSEST
                    )
                }

                if (!paused) {
                    preparedPlayer.start()
                }

                runCatching {
                    onProgress(
                        preparedPlayer.currentPosition.toLong(),
                        preparedPlayer.duration.coerceAtLeast(0).toLong()
                    )
                }

                main.post(ticker)
            }

            mp.setOnCompletionListener {
                finish(null)
            }

            mp.setOnErrorListener { _, _, _ ->
                fail("Couldn't play this recording.")
                true
            }

            mp.prepareAsync()
        } catch (e: Exception) {
            fail(e.message)
        }
    }

    fun pause() {
        paused = true
        runCatching {
            player?.takeIf { it.isPlaying }?.pause()
        }
    }

    fun resume() {
        paused = false
        // start() on a prepared player is valid; before preparation it throws, which is
        // caught here. The onPreparedListener checks the paused flag and starts playback.
        runCatching {
            player?.start()
        }
    }

    fun seekTo(positionMs: Long) {
        if (positionMs < 0L) return

        runCatching {
            player?.seekTo(
                positionMs.coerceAtMost(Int.MAX_VALUE.toLong()),
                MediaPlayer.SEEK_CLOSEST
            )
        }
    }

    fun stop() {
        if (stopped) return
        stopped = true
        release()
    }

    private fun finish(error: String?) {
        if (stopped) return

        stopped = true
        release()
        onFinished(error)
    }

    private fun fail(message: String?) {
        finish(message ?: "Couldn't play this recording.")
    }

    private fun release() {
        main.removeCallbacks(ticker)
        player?.let { mediaPlayer ->
            runCatching { mediaPlayer.release() }
        }
        player = null
    }

    private fun wavHeader(dataBytes: Int): ByteArray {
        val channels = 1
        val bitsPerSample = 16
        val blockAlign = channels * bitsPerSample / 8
        val byteRate = PcmPlaybackFormat.SAMPLE_RATE_HZ * blockAlign

        return ByteBuffer.allocate(44)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                put("RIFF".toByteArray(Charsets.US_ASCII))
                putInt(36 + dataBytes)
                put("WAVE".toByteArray(Charsets.US_ASCII))
                put("fmt ".toByteArray(Charsets.US_ASCII))
                putInt(16)
                putShort(1)                      // PCM
                putShort(channels.toShort())
                putInt(PcmPlaybackFormat.SAMPLE_RATE_HZ)
                putInt(byteRate)
                putShort(blockAlign.toShort())
                putShort(bitsPerSample.toShort())
                put("data".toByteArray(Charsets.US_ASCII))
                putInt(dataBytes)
            }
            .array()
    }
}