package com.nestnote.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val VOICE_SAMPLE_RATE = 16_000
private const val VOICE_CHANNEL_IN = AudioFormat.CHANNEL_IN_MONO
private const val VOICE_ENCODING = AudioFormat.ENCODING_PCM_16BIT

/**
 * Records mono PCM and pipes it directly into the repository's encrypted storage stream.
 * No plaintext recording file is written to disk. The store's existing 25 MiB limit applies.
 */
private class EncryptedPcmRecording(
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

/**
 * Recording UI only. Playback and deletion of finished recordings live in
 * [InlineVoicePlayer], rendered inline in the note preview at each marker.
 *
 * Contract:
 *  - [cursorAtRecordStart] returns the body cursor captured the moment the user taps
 *    Record or Attach WAV. The parent uses it to insert a marker at that position.
 *  - [onRecordingAttached] fires once per completed attachment, passing the id and
 *    the cursor at record-start. The parent should insert `[[voice:id]]` at that cursor.
 *  - [addedThisSession] is owned by the editor; it removes unattached ids on save and all ids on discard.
 *    discards the whole editor session. All attachments created during this session
 *    are removed, any in-flight recording is discarded, and any pending review is
 *    deleted. Pre-existing attachments are never touched.
 */
@Composable
internal fun VoiceNoteControls(
    noteId: Long,
    cursorAtRecordStart: () -> Int,
    onRecordingAttached: (attachmentId: String, cursorAtRecordStart: Int) -> Unit,
    addedThisSession: MutableSet<String>
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val voiceNotes = remember(context) { com.nestnote.app.voicenote.VoiceNoteRepository(context) }
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // Recording state
    var recording by remember { mutableStateOf(false) }
    var startingRecording by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var elapsedRecordingSeconds by remember { mutableIntStateOf(0) }
    var recordingSession by remember { mutableStateOf<EncryptedPcmRecording?>(null) }

    // Review state (finished recording, not yet attached)
    var pendingReviewId by remember(noteId) { mutableStateOf<String?>(null) }
    var pendingReviewName by remember(noteId) { mutableStateOf(defaultVoiceName()) }
    var pendingReviewDurationMs by remember(noteId) { mutableIntStateOf(0) }
    var pendingReviewCursor by remember(noteId) { mutableIntStateOf(0) }
    var confirmDiscardReview by remember { mutableStateOf(false) }

    // Review playback state
    var previewingReview by remember { mutableStateOf(false) }
    var reviewPlaying by remember { mutableStateOf(false) }
    var reviewPaused by remember { mutableStateOf(false) }
    var reviewPositionMs by remember { mutableIntStateOf(0) }
    var reviewDurationMs by remember { mutableIntStateOf(0) }
    var seekingReview by remember { mutableStateOf(false) }
    var pendingReviewSeekMs by remember { mutableIntStateOf(0) }
    var reviewSession by remember { mutableStateOf<PcmVoicePlayback?>(null) }

    // Import state
    var importingAudio by remember { mutableStateOf(false) }

    // Permission state
    var pendingPermission by remember { mutableStateOf(false) }
    var showPermissionSettings by remember { mutableStateOf(false) }

    // Audio focus
    var requestedAudioFocus by remember { mutableStateOf(false) }
    var audioFocusListener by remember {
        mutableStateOf<AudioManager.OnAudioFocusChangeListener?>(null)
    }


    var status by remember { mutableStateOf("") }

    // --- WAV import ---
    val audioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cursor = cursorAtRecordStart()
        importingAudio = true
        status = "Importing audio securely…"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val pcm = context.contentResolver.openInputStream(uri)
                        ?.use { decodePcmWav(it.readBytes()) }
                        ?: throw IOException("Could not open the selected audio file.")
                    if (pcm.isEmpty()) throw IOException("The selected audio file contains no audio.")
                    if (pcm.size > com.nestnote.app.voicenote.VoiceNoteRepository.MAX_RECORDING_BYTES) {
                        throw IOException("Audio exceeds the 25 MiB attachment limit.")
                    }
                    val id = UUID.randomUUID().toString()
                    val duration = pcm.size.toLong() * 1000L / (VOICE_SAMPLE_RATE * 2L)
                    voiceNotes.save(
                        noteId, id, ByteArrayInputStream(pcm), System.currentTimeMillis(),
                        duration, defaultVoiceName()
                    )
                    id
                }
            }
            importingAudio = false
            result.onSuccess { id ->
                addedThisSession.add(id)
                onRecordingAttached(id, cursor)
                status = "Audio attached at cursor position."
            }.onFailure {
                status = it.message ?: "Could not attach this audio file."
            }
        }
    }

    // --- Microphone permission ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when (resolveVoicePermissionResult(granted, pendingPermission)) {
            VoicePermissionDecision.START_RECORDING -> {
                pendingPermission = false
                startingRecording = true
                showPermissionSettings = false
                val cursor = cursorAtRecordStart()
                startVoiceRecording(
                    context, audioManager, voiceNotes,
                    noteId, addedThisSession,
                    onStarted = { session, listener ->
                        startingRecording = false
                        recordingSession = session
                        recording = true
                        recordingPaused = false
                        elapsedRecordingSeconds = 0
                        status = "Recording…"
                        requestedAudioFocus = true
                        audioFocusListener = listener
                        pendingReviewCursor = cursor
                    },
                    onStatus = { msg ->
                        startingRecording = false
                        recordingSession = null
                        recording = false
                        recordingPaused = false
                        if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                        requestedAudioFocus = false
                        audioFocusListener = null
                        status = msg
                    },
                    onInterrupted = {
                        startingRecording = false
                        recordingSession?.stop()
                        recordingSession = null
                        recording = false
                        recordingPaused = false
                        if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                        requestedAudioFocus = false
                        audioFocusListener = null
                        status = "Recording stopped because audio focus was interrupted."
                    }
                )
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

    fun beginRecordingFlow() {
        if (recording || startingRecording || pendingPermission) return
        if (pendingReviewId != null || importingAudio) return
        keyboardController?.hide()
        startingRecording = true
        showPermissionSettings = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val cursor = cursorAtRecordStart()
            startVoiceRecording(
                context, audioManager, voiceNotes,
                noteId, addedThisSession,
                onStarted = { session, listener ->
                    startingRecording = false
                    recordingSession = session
                    recording = true
                    recordingPaused = false
                    elapsedRecordingSeconds = 0
                    status = "Recording…"
                    requestedAudioFocus = true
                    audioFocusListener = listener
                    pendingReviewCursor = cursor
                },
                onStatus = { msg ->
                    startingRecording = false
                    recordingSession = null
                    recording = false
                    recordingPaused = false
                    if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                    requestedAudioFocus = false
                    audioFocusListener = null
                    status = msg
                },
                onInterrupted = {
                    startingRecording = false
                    recordingSession?.stop()
                    recordingSession = null
                    recording = false
                    recordingPaused = false
                    if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                    requestedAudioFocus = false
                    audioFocusListener = null
                    status = "Recording stopped because audio focus was interrupted."
                }
            )
        } else {
            pendingPermission = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopRecording() {
        startingRecording = false
        recordingSession?.stop()
        recording = false
        recordingPaused = false
        if (requestedAudioFocus) {
            abandonVoiceAudioFocus(audioManager, audioFocusListener)
            requestedAudioFocus = false
        }
        status = "Finishing encrypted recording…"
    }

    fun stopReviewPlayback() {
        reviewSession?.stop()
        reviewSession = null
        previewingReview = false
        reviewPlaying = false
        reviewPaused = false
        reviewPositionMs = 0
        reviewDurationMs = 0
        if (requestedAudioFocus) {
            abandonVoiceAudioFocus(audioManager, audioFocusListener)
            requestedAudioFocus = false
        }
    }

    // Recording timer
    LaunchedEffect(recording, recordingPaused) {
        if (recording && !recordingPaused) {
            while (recording && !recordingPaused) {
                delay(1_000)
                if (recording && !recordingPaused) elapsedRecordingSeconds += 1
            }
        }
    }

    // Handle recorder completion -> transition to review
    LaunchedEffect(status) {
        if (status.startsWith("Recording ready for review:")) {
            val id = status.substringAfter("Recording ready for review:").trim()
            val payload = runCatching {
                voiceNotes.listPayloads(noteId).firstOrNull { it.attachmentId == id }
            }.getOrNull()
            recording = false
            recordingPaused = false
            recordingSession = null
            requestedAudioFocus = false
            audioFocusListener = null
            if (payload != null) {
                pendingReviewId = id
                pendingReviewName = defaultVoiceName()
                pendingReviewDurationMs = (
                    ((payload.encryptedSizeBytes - 32L).coerceAtLeast(0L) * 1000L) /
                        (VOICE_SAMPLE_RATE * 2L)
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                status = "Recording ready. Preview and attach, or discard."
            } else {
                status = "The recording could not be loaded for review."
            }
        }
    }

    // Background / dispose safety net
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                pendingPermission = false
                startingRecording = false
                recordingSession?.stop()
                recording = false
                recordingPaused = false
                if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                requestedAudioFocus = false
                audioFocusListener = null
                reviewSession?.stop()
                reviewSession = null
                previewingReview = false
                reviewPlaying = false
                reviewPaused = false
                if (status.startsWith("Recording…") || status.startsWith("Recording paused")) {
                    status = "Recording stopped when the app left the foreground."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recordingSession?.stop()
            reviewSession?.stop()
            if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
        }
    }

    val reducedMotion = rememberSystemReducedMotion()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ---------- Recording pill (in progress) ----------
        if (recording || startingRecording || pendingPermission) {
            val minutes = elapsedRecordingSeconds / 60
            val seconds = elapsedRecordingSeconds % 60
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RecordingDot(active = recording && !recordingPaused, reducedMotion = reducedMotion)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            when {
                                startingRecording || pendingPermission -> "Preparing microphone…"
                                recordingPaused -> "Paused · %02d:%02d".format(minutes, seconds)
                                else -> "Recording · %02d:%02d".format(minutes, seconds)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Encrypted on this device · will attach at cursor",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f)
                        )
                    }
                    FilledIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
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
                        },
                        modifier = Modifier.semantics {
                            contentDescription = if (recordingPaused) "Resume voice recording" else "Pause voice recording"
                            stateDescription = when {
                                !recording -> "Unavailable when not recording"
                                recordingPaused -> "Recording paused"
                                else -> "Recording in progress"
                            }
                        }
                    ) {
                        Icon(
                            if (recordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = null
                        )
                    }
                    FilledIconButton(
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            contentColor = MaterialTheme.colorScheme.primaryContainer
                        ),
                        enabled = recording,
                        onClick = { stopRecording() },
                        modifier = Modifier.semantics {
                            contentDescription = "Stop voice recording"
                            stateDescription = if (recording) "Available" else "Unavailable when not recording"
                        }
                    ) { Icon(Icons.Default.Stop, contentDescription = null) }
                }
            }
        } else if (pendingReviewId == null) {
            // ---------- Idle: Record here / Attach WAV ----------
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    enabled = !importingAudio,
                    onClick = { beginRecordingFlow() },
                    modifier = Modifier.semantics {
                        contentDescription = "Record voice note at cursor position"
                    }
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Record here")
                }
                OutlinedButton(
                    enabled = !importingAudio,
                    onClick = {
                        keyboardController?.hide()
                        audioFileLauncher.launch(
                            arrayOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave")
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Attach WAV audio file at cursor position"
                    }
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (importingAudio) "Importing…" else "Attach WAV")
                }
            }
        }

        // ---------- Review card ----------
        if (pendingReviewId != null) {
            val reviewId = pendingReviewId!!
            val reviewAttachment = remember(reviewId) {
                runCatching {
                    voiceNotes.listPayloads(noteId).firstOrNull { it.attachmentId == reviewId }
                }.getOrNull()
            }
            val shownDuration = if (reviewDurationMs > 0) reviewDurationMs else pendingReviewDurationMs
            val progress = if (shownDuration > 0) {
                (reviewPositionMs.toFloat() / shownDuration).coerceIn(0f, 1f)
            } else 0f

            Card(
                modifier = Modifier.fillMaxWidth().semantics {
                    contentDescription = "Recording review"
                },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Review recording",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            if (previewingReview) {
                                "${formatPlaybackTime(reviewPositionMs)} / ${formatPlaybackTime(shownDuration)}"
                            } else {
                                formatPlaybackTime(pendingReviewDurationMs)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f)
                        )
                    }
                    OutlinedTextField(
                        value = pendingReviewName,
                        onValueChange = { pendingReviewName = it.take(160) },
                        label = { Text("Recording name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (shownDuration > 0 && (previewingReview || reviewPositionMs > 0)) {
                        Slider(
                            value = (if (seekingReview) pendingReviewSeekMs else reviewPositionMs)
                                .coerceIn(0, shownDuration).toFloat(),
                            onValueChange = { value ->
                                seekingReview = true
                                pendingReviewSeekMs = value.toInt().coerceIn(0, shownDuration)
                            },
                            onValueChangeFinished = {
                                val target = pendingReviewSeekMs.coerceIn(0, shownDuration)
                                reviewPositionMs = target
                                reviewSession?.seekTo(target.toLong())
                                seekingReview = false
                            },
                            enabled = previewingReview && reviewSession != null,
                            valueRange = 0f..shownDuration.toFloat(),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Seek recording preview"
                                stateDescription = "${formatPlaybackTime(if (seekingReview) pendingReviewSeekMs else reviewPositionMs)} of ${formatPlaybackTime(shownDuration)}"
                            }
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledIconButton(
                            enabled = reviewAttachment != null,
                            onClick = {
                                if (previewingReview) {
                                    if (reviewPaused) {
                                        reviewSession?.resume()
                                        reviewPaused = false
                                        status = "Preview resumed."
                                    } else {
                                        reviewSession?.pause()
                                        reviewPaused = true
                                        status = "Preview paused."
                                    }
                                } else {
                                    if (reviewAttachment != null) {
                                        stopReviewPlayback()
                                        val focus = requestVoiceAudioFocus(audioManager) {
                                            stopReviewPlayback()
                                            status = "Preview stopped because audio focus was interrupted."
                                        }
                                        if (focus == null) {
                                            status = "Audio focus is unavailable. Try again."
                                        } else {
                                            requestedAudioFocus = true
                                            audioFocusListener = focus
                                            previewingReview = true
                                            reviewPlaying = true
                                            reviewPaused = false
                                            reviewPositionMs = 0
                                            reviewDurationMs = 0
                                            val player = PcmVoicePlayback(
                                                context, readAttachment = voiceNotes::read, attachment = reviewAttachment,
                                                playbackSpeed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                                                    .getFloat(PREF_VOICE_PLAYER_SPEED, 1f),
                                                onProgress = { positionMs, durationMs ->
                                                    if (!seekingReview) {
                                                        reviewPositionMs = positionMs
                                                            .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                        pendingReviewSeekMs = reviewPositionMs
                                                    }
                                                    reviewDurationMs = durationMs
                                                        .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                                }
                                            ) { error ->
                                                reviewSession = null
                                                previewingReview = false
                                                reviewPlaying = false
                                                reviewPaused = false
                                                abandonVoiceAudioFocus(audioManager, focus)
                                                audioFocusListener = null
                                                requestedAudioFocus = false
                                                status = error ?: "Preview finished."
                                            }
                                            reviewSession = player
                                            runCatching { player.start() }.onFailure {
                                                status = it.message ?: "Couldn't preview recording."
                                                stopReviewPlayback()
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.semantics {
                                contentDescription =
                                    if (previewingReview && !reviewPaused) "Pause preview recording"
                                    else "Preview recording"
                            }
                        ) {
                            Icon(
                                if (previewingReview && !reviewPaused) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = null
                            )
                        }
                        Button(
                            enabled = pendingReviewName.isNotBlank() && reviewAttachment != null,
                            onClick = {
                                runCatching {
                                    voiceNotes.attachExisting(
                                        noteId = noteId,
                                        attachmentId = reviewId,
                                        createdAtMillis = System.currentTimeMillis(),
                                        durationMillis = pendingReviewDurationMs.toLong(),
                                        displayName = pendingReviewName.trim()
                                    )
                                }.onSuccess {
                                    stopReviewPlayback()
                                    pendingReviewId = null
                                    addedThisSession.add(reviewId)
                                    onRecordingAttached(reviewId, pendingReviewCursor)
                                    status = "Recording attached at cursor position."
                                }.onFailure {
                                    status = it.message ?: "Couldn't attach reviewed recording."
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("Attach at cursor") }
                        OutlinedButton(onClick = { confirmDiscardReview = true }) {
                            Text("Discard")
                        }
                    }
                }
            }
        }

        // ---------- Status ----------
        if (status.isNotBlank()) {
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (showPermissionSettings) {
            TextButton(onClick = { beginRecordingFlow() }) { Text("Try again") }
            OutlinedButton(onClick = {
                showPermissionSettings = false
                runCatching {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:${context.packageName}")
                        )
                    )
                }.onFailure {
                    status = "Open Android Settings and allow microphone access for NESTNOTE."
                }
            }) { Text("Open app settings") }
        }
    }

    // ---------- Review discard confirmation ----------
    if (confirmDiscardReview && pendingReviewId != null) {
        AlertDialog(
            onDismissRequest = { confirmDiscardReview = false },
            title = { Text("Discard this recording?") },
            text = { Text("The recording will be permanently deleted and cannot be recovered.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscardReview = false
                    val id = pendingReviewId
                    stopReviewPlayback()
                    if (id != null) {
                        addedThisSession.remove(id)
                        runCatching { voiceNotes.delete(noteId, id) }
                    }
                    pendingReviewId = null
                    pendingReviewName = defaultVoiceName()
                    pendingReviewDurationMs = 0
                    pendingReviewCursor = 0
                    status = "Recording discarded. You can record again at any time."
                }) { Text("Discard") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscardReview = false }) { Text("Keep recording") }
            }
        )
    }
}

/**
 * Starts a voice recording session and calls [onStarted] with the live session and the
 * audio-focus listener that must be released when playback or recording ends.
 *
 * The attachment id is registered in [addedIds] before background IO starts so a
 * discard triggered mid-recording can find and clean up the file once the session drains.
 */
private fun startVoiceRecording(
    context: Context,
    audioManager: AudioManager,
    repository: com.nestnote.app.voicenote.VoiceNoteRepository,
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
    addedIds.add(id)
    val session = EncryptedPcmRecording(context, repository, noteId, id) { result ->
        abandonVoiceAudioFocus(audioManager, focusListener)
        // Discard can finish before this main-thread callback is dispatched. Never let
        // a late successful worker callback recreate the manifest entry for discarded audio.
        if (id !in addedIds) {
            runCatching { repository.deletePayload(noteId, id) }
            return@EncryptedPcmRecording
        }
        when {
            result.isSuccess -> {
                // Save encrypted bytes only. Manifest entry is created after explicit Attach.
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
        runCatching { repository.deletePayload(noteId, id) }
        abandonVoiceAudioFocus(audioManager, focusListener)
        onStatus(error.message ?: "Couldn't start recording")
    }
}

// Keep the exact request paired with its listener so every lifecycle path releases
// the same focus request that was granted. minSdk is 26, so the request-based API is
// available on every supported Android version.
private val voiceAudioFocusRequests =
    java.util.Collections.synchronizedMap(
        java.util.WeakHashMap<AudioManager.OnAudioFocusChangeListener, AudioFocusRequest>()
    )

internal fun requestVoiceAudioFocus(
    audioManager: AudioManager,
    onInterrupted: () -> Unit
): AudioManager.OnAudioFocusChangeListener? {
    val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) onInterrupted()
    }
    val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        .setOnAudioFocusChangeListener(listener)
        .build()
    if (audioManager.requestAudioFocus(request) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
        return null
    }
    voiceAudioFocusRequests[listener] = request
    return listener
}

internal fun abandonVoiceAudioFocus(
    audioManager: AudioManager,
    listener: AudioManager.OnAudioFocusChangeListener?
) {
    if (listener == null) return
    val request = voiceAudioFocusRequests.remove(listener) ?: return
    audioManager.abandonAudioFocusRequest(request)
}

/** Converts uncompressed 16-bit mono/stereo WAV to the inline player’s 16 kHz mono PCM format. */
private fun decodePcmWav(bytes: ByteArray): ByteArray {
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
    val outFrames = (frames.toLong() * VOICE_SAMPLE_RATE / sampleRate)
        .coerceAtLeast(1L)
        .coerceAtMost((Int.MAX_VALUE / 2).toLong())
        .toInt()
    val input = java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    val output = java.nio.ByteBuffer.allocate(outFrames * 2).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    repeat(outFrames) { i ->
        val frame = ((i.toLong() * sampleRate) / VOICE_SAMPLE_RATE).toInt().coerceAtMost(frames - 1)
        val left = input.getShort(dataOffset + frame * channels * 2).toInt()
        val sample = if (channels == 2) {
            (left + input.getShort(dataOffset + frame * 4 + 2).toInt()) / 2
        } else left
        output.putShort(i * 2, sample.toShort())
    }
    return output.array()
}

private fun formatPlaybackTime(durationMs: Int): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/** Small pulsing dot shown while a voice recording is active. Decorative. */
@Composable
private fun RecordingDot(active: Boolean, reducedMotion: Boolean) {
    val alpha = if (active && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "recording-dot")
        val pulsing by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse),
            label = "recording-dot-alpha"
        )
        pulsing
    } else {
        1f
    }
    Box(
        Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.error.copy(alpha = alpha))
    )
}
/** e.g. "Voice note · Oct 20, 3:42 PM". Editable in the review step and stored in the manifest. */
internal fun defaultVoiceName(): String =
    "Voice note \u00b7 " + java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date())