package com.nestnote.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.provider.Settings
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.IOException
import java.io.ByteArrayInputStream
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.crypto.Cipher

private const val VOICE_SAMPLE_RATE = 16_000
private const val VOICE_CHANNEL_IN = android.media.AudioFormat.CHANNEL_IN_MONO
private const val VOICE_ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT

/**
 * Records mono PCM and pipes it directly into VoiceAttachmentStore's encrypting stream.
 * No plaintext recording file is written to disk. The store's existing 25 MiB limit applies.
 */
private class EncryptedPcmRecording(
    private val context: Context,
    private val store: VoiceAttachmentStore,
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
        val minBuffer = AudioRecord.getMinBufferSize(VOICE_SAMPLE_RATE, VOICE_CHANNEL_IN, VOICE_ENCODING)
        if (minBuffer <= 0) throw IOException("Microphone recording is unavailable on this device")
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, VOICE_SAMPLE_RATE, VOICE_CHANNEL_IN,
            VOICE_ENCODING, maxOf(minBuffer * 2, 8192)
        )
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
                val saved = store.save(noteId, attachmentId, input)
                val failure = recordingFailure.get()
                if (failure != null || audioBytes.get() == 0L || limitExceeded.get()) {
                    store.delete(noteId, attachmentId)
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
                    maxBytes = VoiceAttachmentStore.MAX_AUDIO_BYTES.toLong(),
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

@Composable
internal fun VoiceNoteControls(
    noteId: Long,
    onAttachmentCountChanged: (Int) -> Unit,
    onVoiceAttachmentAdded: () -> Unit,
    onDiscardHandlerReady: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val attachmentStore = remember(context) { VoiceAttachmentStore(context) }
    val attachmentManifest = remember(context) { VoiceAttachmentManifest(context) }
    var attachments by remember(noteId) { mutableStateOf(runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var importingAudio by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var startingRecording by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var pendingReviewId by remember(noteId) { mutableStateOf<String?>(null) }
    var pendingReviewName by remember(noteId) { mutableStateOf("Voice recording") }
    var pendingReviewDurationMs by remember(noteId) { mutableIntStateOf(0) }
    var elapsedRecordingSeconds by remember { mutableIntStateOf(0) }
    var showPermissionSettings by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var playbackPositionMs by remember { mutableIntStateOf(0) }
    var pendingSeekMs by remember { mutableIntStateOf(0) }
    var seekingPlayback by remember { mutableStateOf(false) }
    var playbackPaused by remember { mutableStateOf(false) }
    var playbackDurationMs by remember { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var pendingPermission by remember { mutableStateOf(false) }
    var recordingSession by remember { mutableStateOf<EncryptedPcmRecording?>(null) }
    var playbackSession by remember { mutableStateOf<PcmVoicePlayback?>(null) }
    var requestedAudioFocus by remember { mutableStateOf(false) }
    var audioFocusListener by remember { mutableStateOf<AudioManager.OnAudioFocusChangeListener?>(null) }
    val addedThisSession = remember(noteId) { mutableSetOf<String>() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val audioFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importingAudio = true
            status = "Importing audio securely…"
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    runCatching {
                        val pcm = context.contentResolver.openInputStream(uri)?.use { decodePcmWav(it.readBytes()) }
                            ?: throw IOException("Could not open the selected audio file.")
                        if (pcm.isEmpty()) throw IOException("The selected audio file contains no audio.")
                        if (pcm.size > VoiceAttachmentStore.MAX_AUDIO_BYTES) throw IOException("Audio exceeds the 25 MiB attachment limit.")
                        val id = UUID.randomUUID().toString()
                        val saved = attachmentStore.save(noteId, id, ByteArrayInputStream(pcm))
                        val duration = pcm.size.toLong() * 1000L / (VOICE_SAMPLE_RATE * 2L)
                        attachmentManifest.put(VoiceAttachmentMetadata(noteId, id, System.currentTimeMillis(), duration, saved.encryptedSizeBytes, "Imported audio"), attachmentStore)
                        saved
                    }
                }
                importingAudio = false
                result.onSuccess {
                    attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
                    onAttachmentCountChanged(attachments.size)
                    onVoiceAttachmentAdded()
                    status = "Audio attached securely."
                }.onFailure { status = it.message ?: "Could not attach this audio file." }
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        when (resolveVoicePermissionResult(granted, pendingPermission)) {
            VoicePermissionDecision.START_RECORDING -> {
                pendingPermission = false
                startingRecording = true
                showPermissionSettings = false
                startVoiceRecording(context, audioManager, attachmentStore, attachmentManifest, noteId, addedThisSession,
                    { session, listener -> startingRecording = false; recordingSession = session; recording = true; recordingPaused = false; elapsedRecordingSeconds = 0; status = "Recording…"; requestedAudioFocus = true; audioFocusListener = listener },
                    { message ->
                        startingRecording = false
                        recordingSession = null
                        recording = false
                        recordingPaused = false
                        if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener)
                        requestedAudioFocus = false
                        audioFocusListener = null
                        status = message
                    },
                    { startingRecording = false; recordingSession?.stop(); recordingSession = null; recording = false; recordingPaused = false; if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener); requestedAudioFocus = false; audioFocusListener = null; status = "Recording stopped because audio focus was interrupted. Tap Record to try again." })
            }
            VoicePermissionDecision.SHOW_PERMISSION_GUIDANCE -> {
                pendingPermission = false
                startingRecording = false
                showPermissionSettings = true
                status = "Microphone permission was denied. Allow it in app settings to record voice notes."
            }
            VoicePermissionDecision.IGNORE_STALE_RESULT -> Unit
        }
    }

    fun stopRecording() {
        startingRecording = false
        recordingSession?.stop()
        recording = false
        recordingPaused = false
        if (requestedAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusListener)
            requestedAudioFocus = false
        }
        status = "Finishing encrypted recording…"
    }
    fun stopPlayback() {
        playbackSession?.stop()
        playbackSession = null
        playingId = null
        playbackPositionMs = 0
        playbackDurationMs = 0
        pendingSeekMs = 0
        seekingPlayback = false
        playbackPaused = false
        if (requestedAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusListener)
            requestedAudioFocus = false
        }
    }
    LaunchedEffect(recording, recordingPaused) {
        if (recording && !recordingPaused) {
            while (recording && !recordingPaused) {
                kotlinx.coroutines.delay(1_000)
                if (recording && !recordingPaused) elapsedRecordingSeconds += 1
            }
        }
    }

    LaunchedEffect(noteId) {
        attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
        onAttachmentCountChanged(attachments.size)
    }
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                // A permission sheet or asynchronous microphone startup may still be in
                // flight when the editor backgrounds. Invalidate both states so a stale
                // permission result cannot leave Record permanently disabled.
                pendingPermission = false
                startingRecording = false
                recordingSession?.stop()
                recording = false
                recordingPaused = false
                if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener)
                requestedAudioFocus = false
                audioFocusListener = null
                playbackSession?.stop()
                playbackSession = null
                playingId = null
                playbackPaused = false
                status = "Recording/playback stopped when the app left the foreground."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recordingSession?.stop()
            playbackSession?.stop()
            if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    LaunchedEffect(status) {
        if (status.startsWith("Recording ready for review:")) {
            val id = status.substringAfter("Recording ready for review:").trim()
            val payload = runCatching { attachmentStore.list(noteId).firstOrNull { it.attachmentId == id } }.getOrNull()
            recording = false
            recordingPaused = false
            recordingSession = null
            requestedAudioFocus = false
            audioFocusListener = null
            if (payload != null) {
                pendingReviewId = id
                pendingReviewName = "Voice recording"
                pendingReviewDurationMs = (((payload.encryptedSizeBytes - 32L).coerceAtLeast(0L) * 1000L) / (VOICE_SAMPLE_RATE * 2L)).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                status = "Recording ready. Preview and attach it when you're satisfied."
            } else {
                status = "The recording could not be loaded for review."
            }
        }
    }

    androidx.compose.runtime.SideEffect {
        onDiscardHandlerReady {
            recordingSession?.let { session ->
                recordingSession = null
                // Remove identities first: completion callbacks already queued on the main
                // thread will observe the discard and cannot recreate manifest entries.
                addedThisSession.toList().forEach { id ->
                    runCatching { attachmentStore.delete(noteId, id) }
                    runCatching { attachmentManifest.remove(noteId, id) }
                }
                addedThisSession.clear()
                session.discardAndWait()
            }
            recordingSession = null
            recording = false
            recordingPaused = false
            addedThisSession.toList().forEach { id ->
                runCatching { attachmentStore.delete(noteId, id) }
                runCatching { attachmentManifest.remove(noteId, id) }
            }
            addedThisSession.clear()
            attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
            onAttachmentCountChanged(attachments.size)
        }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        ) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Voice", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedButton(
                    enabled = !recording && !startingRecording && !pendingPermission && !importingAudio,
                    onClick = { audioFileLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")) }
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach audio file")
                    Text(if (importingAudio) "Importing…" else "Attach audio file")
                }
                Button(
                    modifier = Modifier.semantics {
                        contentDescription = "Record voice note"
                        stateDescription = when {
                            recording -> "Recording in progress"
                            startingRecording -> "Preparing microphone"
                            pendingPermission -> "Waiting for microphone permission"
                            playingId != null -> "Unavailable while playback is active"
                            else -> "Ready"
                        }
                    },
                    enabled = canStartVoiceRecording(recording, startingRecording, pendingPermission, playingId != null || pendingReviewId != null),
                    onClick = {
                        keyboardController?.hide()
                        if (!canStartVoiceRecording(recording, startingRecording, pendingPermission, playingId != null || pendingReviewId != null)) return@Button
                        startingRecording = true
                        showPermissionSettings = false
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceRecording(context, audioManager, attachmentStore, attachmentManifest, noteId, addedThisSession,
                                { session, listener -> startingRecording = false; recordingSession = session; recording = true; recordingPaused = false; elapsedRecordingSeconds = 0; status = "Recording…"; requestedAudioFocus = true; audioFocusListener = listener },
                                { message ->
                                    startingRecording = false
                                    recordingSession = null
                                    recording = false
                                    recordingPaused = false
                                    if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener)
                                    requestedAudioFocus = false
                                    audioFocusListener = null
                                    status = message
                                },
                                { startingRecording = false; recordingSession?.stop(); recordingSession = null; recording = false; requestedAudioFocus = false; audioFocusListener = null; status = "Recording stopped because audio focus was interrupted." })
                        } else {
                            pendingPermission = true
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Record voice note")
                    Text(if (startingRecording || pendingPermission) "Starting…" else "Record")
                }
                OutlinedButton(
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                    modifier = Modifier.semantics {
                        contentDescription = if (recordingPaused) "Resume voice recording" else "Pause voice recording"
                        stateDescription = when {
                            !recording -> "Unavailable when not recording"
                            recordingPaused -> "Recording paused"
                            else -> "Recording in progress"
                        }
                    },
                    enabled = recording,
                    onClick = {
                        if (recordingPaused) {
                            runCatching { recordingSession?.resume() }
                                .onSuccess { recordingPaused = false; status = "Recording…" }
                                .onFailure { stopRecording(); status = it.message ?: "Couldn't resume recording" }
                        } else {
                            recordingSession?.pause()
                            recordingPaused = true
                            status = "Recording paused."
                        }
                    }
                ) {
                    Icon(if (recordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = if (recordingPaused) "Resume recording" else "Pause recording")
                    Text(if (recordingPaused) "Resume" else "Pause")
                }
                OutlinedButton(
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)),
                    modifier = Modifier.semantics {
                        contentDescription = "Stop voice recording"
                        stateDescription = if (recording) "Available" else "Unavailable when not recording"
                    },
                    enabled = recording,
                    onClick = { stopRecording() }
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop recording")
                    Text("Stop")
                }
                }
                }
            if (recording) {
                val minutes = elapsedRecordingSeconds / 60
                val seconds = elapsedRecordingSeconds % 60
                Text(
                    if (recordingPaused) "Recording paused • %02d:%02d".format(minutes, seconds)
                    else "Recording • %02d:%02d".format(minutes, seconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else if (startingRecording || pendingPermission) {
                Text("Preparing microphone…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (pendingReviewId != null) {
                val reviewId = pendingReviewId!!
                val reviewAttachment = attachments.firstOrNull { it.attachmentId == reviewId }
                    ?: runCatching { attachmentStore.list(noteId).firstOrNull { it.attachmentId == reviewId } }.getOrNull()
                Card(
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Recording review" },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Review recording", style = MaterialTheme.typography.titleSmall)
                        OutlinedTextField(value = pendingReviewName, onValueChange = { pendingReviewName = it.take(160) }, label = { Text("Recording name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Text("Preview • ${formatPlaybackTime(pendingReviewDurationMs)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                enabled = reviewAttachment != null && !recording && (playingId == null || playingId == reviewId),
                                onClick = {
                                    if (playingId == reviewId) {
                                        if (playbackPaused) { playbackSession?.resume(); playbackPaused = false; status = "Preview resumed." }
                                        else { playbackSession?.pause(); playbackPaused = true; status = "Preview paused." }
                                    } else if (reviewAttachment != null) {
                                        stopPlayback()
                                        val focus = requestVoiceAudioFocus(audioManager) { stopPlayback(); status = "Preview stopped because audio focus was interrupted." }
                                        if (focus == null) status = "Audio focus is unavailable. Try again."
                                        else {
                                            requestedAudioFocus = true
                                            audioFocusListener = focus
                                            playingId = reviewId
                                            playbackPaused = false
                                            playbackPositionMs = 0
                                            playbackDurationMs = 0
                                            val player = PcmVoicePlayback(context, attachmentStore, reviewAttachment, onProgress = { positionMs, durationMs ->
                                                playbackPositionMs = positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                playbackDurationMs = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                            }) { error ->
                                                playingId = null; playbackSession = null; playbackPaused = false
                                                audioManager.abandonAudioFocus(focus); audioFocusListener = null; requestedAudioFocus = false
                                                status = error ?: "Preview finished."
                                            }
                                            playbackSession = player
                                            runCatching { player.start() }.onFailure { status = it.message ?: "Couldn't preview recording." }
                                        }
                                    }
                                }
                            ) { Icon(if (playingId == reviewId && !playbackPaused) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null); Text(if (playingId == reviewId && !playbackPaused) "Pause preview" else "Preview") }
                            Button(enabled = pendingReviewName.isNotBlank() && reviewAttachment != null, onClick = {
                                runCatching {
                                    attachmentManifest.put(VoiceAttachmentMetadata(noteId, reviewId, System.currentTimeMillis(), pendingReviewDurationMs.toLong(), reviewAttachment!!.encryptedSizeBytes, pendingReviewName.trim()), attachmentStore)
                                }.onSuccess {
                                    pendingReviewId = null
                                    attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
                                    onAttachmentCountChanged(attachments.size)
                                    onVoiceAttachmentAdded()
                                    status = "Recording attached to this note."
                                }.onFailure { status = it.message ?: "Couldn't attach reviewed recording." }
                            }) { Text("Attach") }
                            OutlinedButton(onClick = {
                                if (playingId == reviewId) stopPlayback()
                                addedThisSession.remove(reviewId)
                                runCatching { attachmentStore.delete(noteId, reviewId) }
                                runCatching { attachmentManifest.remove(noteId, reviewId) }
                                pendingReviewId = null
                                pendingReviewName = "Voice recording"
                                pendingReviewDurationMs = 0
                                attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
                                onAttachmentCountChanged(attachments.size)
                                status = "Reviewed recording discarded. Existing attachments were kept."
                            }) { Text("Discard") }
                        }
                    }
                }
            }
            val visibleAttachments = attachments.filter { it.attachmentId != pendingReviewId }
            if (visibleAttachments.isEmpty() && pendingReviewId == null) {
                Text("No audio attachments yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                visibleAttachments.forEachIndexed { index, attachment ->
                    val active = playingId == attachment.attachmentId
                    val shownDuration = if (active && playbackDurationMs > 0) playbackDurationMs else 0
                    val shownPosition = if (active) playbackPositionMs else 0
                    Card(
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Audio attachment ${index + 1}"
                        },
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                IconButton(
                                    enabled = !recording && (playingId == null || active),
                                    onClick = {
                                        if (active) {
                                            if (playbackPaused) {
                                                playbackSession?.resume()
                                                playbackPaused = false
                                                status = "Playback resumed."
                                            } else {
                                                playbackSession?.pause()
                                                playbackPaused = true
                                                status = "Playback paused."
                                            }
                                        } else {
                                            stopPlayback()
                                            val focus = requestVoiceAudioFocus(audioManager) {
                                                stopPlayback()
                                                status = "Playback stopped because audio focus was interrupted."
                                            }
                                            if (focus == null) status = "Audio focus is unavailable. Try again."
                                            else {
                                                requestedAudioFocus = true
                                                audioFocusListener = focus
                                                playingId = attachment.attachmentId
                                                playbackPaused = false
                                                playbackPositionMs = 0
                                                pendingSeekMs = 0
                                                playbackDurationMs = 0
                                                status = "Playing recording ${index + 1}."
                                                val player = PcmVoicePlayback(
                                                    context,
                                                    attachmentStore,
                                                    attachment,
                                                    onProgress = { positionMs, durationMs ->
                                                        playbackPositionMs = positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                        playbackDurationMs = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                        if (!seekingPlayback) pendingSeekMs = playbackPositionMs
                                                    }
                                                ) { error ->
                                                    playingId = null
                                                    playbackSession = null
                                                    playbackPaused = false
                                                    audioManager.abandonAudioFocus(focus)
                                                    audioFocusListener = null
                                                    requestedAudioFocus = false
                                                    status = error ?: "Playback finished."
                                                }
                                                playbackSession = player
                                                runCatching { player.start() }.onFailure { error ->
                                                    playbackSession = null
                                                    playingId = null
                                                    playbackPaused = false
                                                    audioManager.abandonAudioFocus(focus)
                                                    audioFocusListener = null
                                                    requestedAudioFocus = false
                                                    status = error.message ?: "Couldn't start playback. Try again."
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier.semantics {
                                        contentDescription = if (active && !playbackPaused) "Pause recording ${index + 1}" else "Play recording ${index + 1}"
                                    }
                                ) {
                                    Icon(
                                        if (active && !playbackPaused) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null
                                    )
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        if (attachment.attachmentId in addedThisSession) "Recording ${index + 1} · Not yet saved to note" else "Recording ${index + 1}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        if (shownDuration > 0) formatPlaybackTime(shownDuration) else formatVoiceDuration(attachment.encryptedSizeBytes),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            val estimatedDurationMs = (((attachment.encryptedSizeBytes - 32L).coerceAtLeast(0L) * 1000L) / (VOICE_SAMPLE_RATE * 2L))
                                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                            val sliderMax = maxOf(if (active && playbackDurationMs > 0) playbackDurationMs else estimatedDurationMs, 1)
                            Slider(
                                value = if (active) pendingSeekMs.coerceIn(0, sliderMax).toFloat() else 0f,
                                onValueChange = { value ->
                                    if (active && playbackDurationMs > 0) {
                                        seekingPlayback = true
                                        pendingSeekMs = value.toInt()
                                    }
                                },
                                onValueChangeFinished = {
                                    if (active && playbackDurationMs > 0) {
                                        playbackPositionMs = pendingSeekMs
                                        playbackSession?.seekTo(pendingSeekMs.toLong())
                                    }
                                    seekingPlayback = false
                                },
                                valueRange = 0f..sliderMax.toFloat(),
                                enabled = active && playbackDurationMs > 0 && !recording,
                                modifier = Modifier.fillMaxWidth().height(28.dp).semantics {
                                    contentDescription = "Seek recording ${index + 1}"
                                }
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(formatPlaybackTime(if (active) shownPosition else 0), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    if (active && playbackDurationMs > 0) formatPlaybackTime(playbackDurationMs)
                                    else formatPlaybackTime(estimatedDurationMs),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showPermissionSettings) {
                OutlinedButton(onClick = {
                    showPermissionSettings = false
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }.onFailure { status = "Open Android Settings and allow microphone access for NESTNOTE." }
                }) {
                    Text("Open app settings")
                }
            }
        }
    }
}
}

/** Converts uncompressed 16-bit mono/stereo WAV to the inline player’s 16 kHz mono PCM format. */
private fun decodePcmWav(bytes: ByteArray): ByteArray {
    if (bytes.size < 44 || String(bytes, 0, 4, Charsets.US_ASCII) != "RIFF" || String(bytes, 8, 4, Charsets.US_ASCII) != "WAVE") throw IOException("Choose an uncompressed PCM WAV file (16-bit audio).")
    var offset = 12; var format = -1; var channels = 0; var sampleRate = 0; var bits = 0; var dataOffset = -1; var dataLength = 0
    while (offset + 8 <= bytes.size) {
        val id = String(bytes, offset, 4, Charsets.US_ASCII)
        val length = java.nio.ByteBuffer.wrap(bytes, offset + 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int
        if (length < 0 || offset + 8L + length > bytes.size) throw IOException("The WAV file is malformed.")
        if (id == "fmt " && length >= 16) { val b = java.nio.ByteBuffer.wrap(bytes, offset + 8, length).order(java.nio.ByteOrder.LITTLE_ENDIAN); format = b.short.toInt() and 0xffff; channels = b.short.toInt() and 0xffff; sampleRate = b.int; b.int; b.short; bits = b.short.toInt() and 0xffff }
        if (id == "data") { dataOffset = offset + 8; dataLength = length }
        offset += 8 + length + (length and 1)
    }
    if (format != 1 || channels !in 1..2 || bits != 16 || sampleRate <= 0 || dataOffset < 0 || dataLength < channels * 2) throw IOException("Unsupported WAV format. Use uncompressed 16-bit mono or stereo WAV.")
    val frames = dataLength / (channels * 2); val outFrames = (frames.toLong() * VOICE_SAMPLE_RATE / sampleRate).coerceAtLeast(1L).coerceAtMost((Int.MAX_VALUE / 2).toLong()).toInt()
    val input = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN); val output = java.nio.ByteBuffer.allocate(outFrames * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    repeat(outFrames) { i -> val frame = ((i.toLong() * sampleRate) / VOICE_SAMPLE_RATE).toInt().coerceAtMost(frames - 1); val left = input.getShort(dataOffset + frame * channels * 2).toInt(); val sample = if (channels == 2) (left + input.getShort(dataOffset + frame * 4 + 2).toInt()) / 2 else left; output.putShort(i * 2, sample.toShort()) }
    return output.array()
}
private fun formatPlaybackTime(durationMs: Int): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

private fun startVoiceRecording(
    context: Context,
    audioManager: AudioManager,
    store: VoiceAttachmentStore,
    manifest: VoiceAttachmentManifest,
    noteId: Long,
    addedIds: MutableSet<String>,
    onStarted: (EncryptedPcmRecording, AudioManager.OnAudioFocusChangeListener) -> Unit,
    onStatus: (String) -> Unit,
    onInterrupted: () -> Unit
) {
    val focusListener = requestVoiceAudioFocus(audioManager, onInterrupted)
    if (focusListener == null) {
        onStatus("Audio focus is unavailable. Stop other audio and try again.")
        return
    }
    val id = UUID.randomUUID().toString()
    // Register the reserved attachment identity before background IO starts, so a discard can
    // wait for finalization and remove a file even if its UI callback has not run yet.
    addedIds.add(id)
    val session = EncryptedPcmRecording(context, store, noteId, id) { result ->
        audioManager.abandonAudioFocus(focusListener)
        // Discard can finish before this main-thread callback is dispatched. Never let a
        // late successful worker callback recreate the manifest entry for discarded audio.
        if (id !in addedIds) {
            runCatching { store.delete(noteId, id) }
            runCatching { manifest.remove(noteId, id) }
            return@EncryptedPcmRecording
        }
        when {
            result.isSuccess -> {
                // Save encrypted bytes only. The manifest entry is created after explicit Attach.
                onStatus("Recording ready for review: $id")
            }
            else -> onStatus(result.exceptionOrNull()?.message ?: "Couldn't save recording")
        }
    }
    try {
        session.start()
        onStarted(session, focusListener)
    } catch (error: Exception) {
        // A partially started session may already have a storage worker. Mark this
        // attachment discarded first, then wait for workers before deleting its payload.
        addedIds.remove(id)
        runCatching { session.discardAndWait() }
        runCatching { store.delete(noteId, id) }
        runCatching { manifest.remove(noteId, id) }
        audioManager.abandonAudioFocus(focusListener)
        onStatus(error.message ?: "Couldn't start recording")
    }
}

@Suppress("DEPRECATION")
private fun requestVoiceAudioFocus(
    audioManager: AudioManager,
    onInterrupted: () -> Unit
): AudioManager.OnAudioFocusChangeListener? {
    val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change <= AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) onInterrupted()
    }
    return if (audioManager.requestAudioFocus(
        listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
    ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) listener else null
}
