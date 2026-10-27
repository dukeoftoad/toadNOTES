package com.nestnote.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
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
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Recording UI only. Playback and deletion of finished recordings live in
 * [InlineVoicePlayer], rendered inline in the note preview at each marker.
 *
 * Contract:
 *  - [cursorAtRecordStart] returns the body cursor captured the moment the user taps
 *    Record or Import audio. The parent uses it to insert a marker at that position.
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

    // --- Audio import ---
    val audioImporter = remember(context, voiceNotes) {
        com.nestnote.app.voicenote.VoiceNoteImporter(context, voiceNotes)
    }
    val audioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cursor = cursorAtRecordStart()
        importingAudio = true
        status = "Importing audio securely…"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { audioImporter.importAudio(noteId, uri).attachmentId }
            }
            importingAudio = false
            result.onSuccess { id ->
                addedThisSession.add(id)
                onRecordingAttached(id, cursor)
                status = "Audio imported and attached at cursor position. Playback depends on Android codec support."
            }.onFailure {
                status = it.message ?: "Could not import this audio file."
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
            // ---------- Idle: Record here / Import audio ----------
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
                        // "audio/*" alone hides dialer recordings whose provider reports a
                        // non-audio MIME (e.g. OGG as application/ogg, FLAC as
                        // application/x-flac). VoiceAudioImportPolicy still validates every
                        // selection by extension/MIME before import.
                        audioFileLauncher.launch(
                            arrayOf("audio/*", "application/ogg", "application/x-flac")
                        )
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Import audio file at cursor position"
                    }
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (importingAudio) "Importing…" else "Import audio")
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