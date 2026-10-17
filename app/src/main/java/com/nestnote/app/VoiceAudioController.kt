package com.nestnote.app

import android.content.Context
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.IOException
import java.util.UUID

/** Foreground-only audio engine. Caller must obtain RECORD_AUDIO permission before startRecording. */
class VoiceAudioController(context: Context, private val store: VoiceAttachmentStore, private val manifest: VoiceAttachmentManifest) {
    enum class State { IDLE, RECORDING, PAUSED, PLAYING, ERROR }
    private val appContext = context.applicationContext
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recordingFile: File? = null
    private var activeNoteId: Long? = null
    var state: State = State.IDLE
        private set

    @Synchronized @Throws(IOException::class, SecurityException::class)
    fun startRecording(noteId: Long) {
        require(noteId > 0)
        stopPlayback()
        check(state != State.RECORDING && state != State.PAUSED) { "Recording already active" }
        val file = File(appContext.cacheDir, "voice-" + UUID.randomUUID() + ".tmp")
        val next = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(appContext) else MediaRecorder()
        try {
            next.setAudioSource(MediaRecorder.AudioSource.MIC)
            next.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            next.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            next.setAudioEncodingBitRate(96000)
            next.setAudioSamplingRate(44100)
            next.setOutputFile(file.absolutePath)
            next.prepare()
            next.start()
            recorder = next; recordingFile = file; activeNoteId = noteId; state = State.RECORDING
        } catch (error: Exception) {
            runCatching { next.reset() }; runCatching { next.release() }; file.delete()
            state = State.ERROR
            throw IOException("Could not start voice recording", error)
        }
    }

    @Synchronized fun pauseRecording() {
        if (state != State.RECORDING) return
        if (Build.VERSION.SDK_INT < 24) throw UnsupportedOperationException("Pause requires Android 7.0 or newer")
        try { recorder?.pause(); state = State.PAUSED }
        catch (error: Exception) { abortRecording(); throw IOException("Recording pause failed", error) }
    }

    @Synchronized fun resumeRecording() {
        if (state != State.PAUSED) return
        try { recorder?.resume(); state = State.RECORDING }
        catch (error: Exception) { abortRecording(); throw IOException("Recording resume failed", error) }
    }

    @Synchronized @Throws(IOException::class)
    fun stopAndSave(displayName: String = "Voice note"): VoiceAttachmentMetadata {
        val noteId = activeNoteId ?: throw IllegalStateException("No recording is active")
        val file = recordingFile ?: throw IllegalStateException("No recording file")
        val active = recorder ?: throw IllegalStateException("Recorder unavailable")
        try {
            active.stop(); active.reset(); active.release(); recorder = null
            val probe = MediaPlayer()
            val duration = try { probe.setDataSource(file.absolutePath); probe.prepare(); probe.duration.toLong().coerceAtLeast(0L) }
                catch (e: Exception) { throw IOException("Recorded audio container is invalid", e) }
                finally { runCatching { probe.release() } }
            if (file.length() <= 0L || file.length() > VoiceAttachmentStore.MAX_AUDIO_BYTES) throw IOException("Recording is empty or exceeds the audio size limit")
            val id = UUID.randomUUID().toString()
            val attachment = file.inputStream().use { store.save(noteId, id, it) }
            val metadata = VoiceAttachmentMetadata(noteId, id, System.currentTimeMillis(), duration,
                attachment.encryptedSizeBytes, displayName.trim().ifBlank { "Voice note" }.take(160))
            manifest.put(metadata, store)
            state = State.IDLE
            return metadata
        } catch (error: Exception) {
            state = State.ERROR
            throw if (error is IOException) error else IOException("Could not safely save recording", error)
        } finally {
            file.delete(); recordingFile = null; activeNoteId = null
            runCatching { recorder?.release() }; recorder = null
            if (state != State.ERROR) state = State.IDLE
        }
    }

    @Synchronized fun abortRecording() {
        runCatching { recorder?.stop() }; runCatching { recorder?.reset() }; runCatching { recorder?.release() }
        recorder = null; recordingFile?.delete(); recordingFile = null; activeNoteId = null; state = State.IDLE
    }

    @Synchronized @Throws(IOException::class)
    fun play(noteId: Long, attachmentId: String, onFinished: () -> Unit = {}) {
        stopPlayback()
        val bytes = store.read(noteId, attachmentId)
        val next = MediaPlayer()
        try {
            next.setDataSource(object : MediaDataSource() {
                override fun getSize(): Long = bytes.size.toLong()
                override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
                    if (position >= bytes.size) return -1
                    val count = minOf(size, bytes.size - position.toInt())
                    System.arraycopy(bytes, position.toInt(), buffer, offset, count)
                    return count
                }
                override fun close() = Unit
            })
            next.setOnCompletionListener { finished ->
                synchronized(this) { runCatching { finished.release() }; if (player === finished) player = null; state = State.IDLE }
                onFinished()
            }
            next.setOnErrorListener { failed, _, _ -> synchronized(this) { runCatching { failed.release() }; if (player === failed) player = null; state = State.ERROR }; true }
            next.prepare(); next.start(); player = next; state = State.PLAYING
        } catch (error: Exception) {
            runCatching { next.release() }; state = State.ERROR
            throw IOException("Could not play authenticated voice attachment", error)
        }
    }

    @Synchronized fun pausePlayback() { val active = player ?: return; if (active.isPlaying) { active.pause(); state = State.IDLE } }
    @Synchronized fun stopPlayback() {
        val active = player ?: return
        runCatching { active.stop() }; runCatching { active.release() }; player = null
        if (state == State.PLAYING) state = State.IDLE
    }
    @Synchronized fun close() { abortRecording(); stopPlayback() }
}
