package com.nestnote.app

import android.media.AudioManager
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Raw PCM byte rate for the voice-note format. Envelope overhead (32 bytes)
 * is subtracted before dividing, so duration estimates match what the player
 * actually decodes.
 */
private const val VOICE_PCM_BYTES_PER_SECOND = 32_000L // 16 kHz × 2 bytes/sample
private const val VOICE_ENVELOPE_OVERHEAD = 32L        // 4 magic + 12 IV + 16 GCM tag

/**
 * Inline voice player, rendered in the note preview at the marker's position.
 *
 * Playback ownership is delegated to the parent so only one voice note plays
 * at a time. The parent owns a single `activePlayerId: String?`; each player
 * observes it and starts/stops accordingly. Pause, resume and seek stay local
 * because they never cross the boundary between players.
 *
 *  - [onPlayRequested] fires when the user taps play on an inactive player.
 *    The parent must set `activePlayerId = attachmentId`. Recomposing with
 *    `activePlayerId == attachmentId` is what actually starts playback here.
 *  - [onStopRequested] fires on natural completion, audio-focus loss, or
 *    lifecycle backgrounding. The parent must set `activePlayerId = null`.
 *  - [onDeleteRequested] fires when the user taps the trash icon. The parent
 *    removes the marker from the body, deletes the manifest entry and the
 *    encrypted payload, and clears `activePlayerId` if it was this attachment.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun InlineVoicePlayer(
    noteId: Long,
    attachmentId: String,
    activePlayerId: String?,
    onPlayRequested: (attachmentId: String) -> Unit,
    onStopRequested: () -> Unit,
    onDeleteRequested: (attachmentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val store = remember(context) { VoiceAttachmentStore(context) }
    val manifest = remember(context) { VoiceAttachmentManifest(context) }
    val audioManager = remember(context) {
        context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager
    }

    // File + metadata lookups recompute only when identity changes.
    val attachment = remember(noteId, attachmentId) {
        runCatching { store.list(noteId).firstOrNull { it.attachmentId == attachmentId } }
            .getOrNull()
    }
    val metadata = remember(noteId, attachmentId) {
        runCatching { manifest.list(noteId, store).firstOrNull { it.attachmentId == attachmentId } }
            .getOrNull()
    }

    val estimatedDurationMs = remember(attachment) {
        attachment?.let {
            (((it.encryptedSizeBytes - VOICE_ENVELOPE_OVERHEAD).coerceAtLeast(0L) * 1000L) /
                VOICE_PCM_BYTES_PER_SECOND)
                .coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
        } ?: 0
    }
    val fallbackDurationMs = metadata?.durationMillis
        ?.takeIf { it > 0L }
        ?.coerceAtMost(Int.MAX_VALUE.toLong())
        ?.toInt()
        ?: estimatedDurationMs

    val isActive = activePlayerId == attachmentId
    val playbackSpeed = remember(context) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getFloat(PREF_VOICE_PLAYER_SPEED, 1f)
    }

    // All playback state is keyed by attachmentId so a recompose with a different
    // marker at the same slot never leaks stale state between voice notes.
    var player by remember(attachmentId) { mutableStateOf<PcmVoicePlayback?>(null) }
    var paused by remember(attachmentId) { mutableStateOf(false) }
    var positionMs by remember(attachmentId) { mutableIntStateOf(0) }
    var durationMs by remember(attachmentId) { mutableIntStateOf(0) }
    var pendingSeekMs by remember(attachmentId) { mutableIntStateOf(0) }
    var seekingPlayback by remember(attachmentId) { mutableStateOf(false) }
    var audioFocusListener by remember(attachmentId) {
        mutableStateOf<AudioManager.OnAudioFocusChangeListener?>(null)
    }

    // Playback lifecycle driven by the parent's active flag.
    LaunchedEffect(isActive, attachment) {
        if (isActive && player == null && attachment != null) {
            val focus = requestVoiceAudioFocus(audioManager) {
                // Focus was taken by another app or an interruption. Stop cleanly
                // and tell the parent to release the slot.
                player?.stop()
                player = null
                paused = false
                positionMs = 0
                durationMs = 0
                audioFocusListener = null
                onStopRequested()
            }
            if (focus == null) {
                onStopRequested()
                return@LaunchedEffect
            }
            audioFocusListener = focus
            val next = PcmVoicePlayback(
                context,
                store,
                attachment,
                playbackSpeed = playbackSpeed,
                onProgress = { pos, dur ->
                    positionMs = pos.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    durationMs = dur.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                    if (!seekingPlayback) pendingSeekMs = positionMs
                }
            ) { _ ->
                // Natural completion or playback error.
                player = null
                paused = false
                positionMs = 0
                durationMs = 0
                abandonVoiceAudioFocus(audioManager, audioFocusListener)
                audioFocusListener = null
                onStopRequested()
            }
            player = next
            runCatching { next.start() }.onFailure {
                player = null
                abandonVoiceAudioFocus(audioManager, focus)
                audioFocusListener = null
                onStopRequested()
            }
        } else if (!isActive && player != null) {
            // Another player was activated. Stop quietly; the parent already
            // switched the slot to somebody else.
            player?.stop()
            player = null
            paused = false
            positionMs = 0
            durationMs = 0
            abandonVoiceAudioFocus(audioManager, audioFocusListener)
            audioFocusListener = null
        }
    }

    // Foreground-only safety net. Voice recordings are never supposed to keep
    // playing while the app is backgrounded, even if the parent forgot to clear
    // activePlayerId.
    DisposableEffect(lifecycleOwner, attachmentId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && player != null) {
                player?.stop()
                player = null
                paused = false
                positionMs = 0
                durationMs = 0
                abandonVoiceAudioFocus(audioManager, audioFocusListener)
                audioFocusListener = null
                onStopRequested()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player?.stop()
            player = null
            abandonVoiceAudioFocus(audioManager, audioFocusListener)
            audioFocusListener = null
        }
    }

    // Marker exists but the underlying encrypted payload is gone. Render a
    // self-healing "missing" card so the user can clean up the marker.
    if (attachment == null) {
        MissingVoiceAttachmentCard(
            attachmentId = attachmentId,
            onDeleteRequested = onDeleteRequested,
            modifier = modifier
        )
        return
    }

    val shownDuration = if (durationMs > 0) durationMs else fallbackDurationMs
    val reducedMotion = rememberSystemReducedMotion()
    val displayName = metadata?.displayName?.takeIf { it.isNotBlank() } ?: "Voice recording"
    var selected by remember(attachmentId) { mutableStateOf(false) }

    val compactPlayer = remember(context) {
        context.getSharedPreferences(PREFS, android.content.Context.MODE_PRIVATE)
            .getBoolean(PREF_VOICE_PLAYER_COMPACT, true)
    }
    if (compactPlayer) {
        Surface(
            modifier = modifier.fillMaxWidth().combinedClickable(
                onClick = { if (selected) selected = false },
                onLongClick = { selected = !selected }
            ).semantics { contentDescription = "Voice note: $displayName" },
            shape = RoundedCornerShape(50),
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledIconButton(
                    onClick = {
                        if (isActive) {
                            if (paused) runCatching { player?.resume() }.onSuccess { paused = false }
                            else runCatching { player?.pause() }.onSuccess { paused = true }
                        } else onPlayRequested(attachmentId)
                    },
                    modifier = Modifier.size(36.dp).semantics {
                        contentDescription = when {
                            !isActive -> "Play $displayName"
                            paused -> "Resume $displayName"
                            else -> "Pause $displayName"
                        }
                    }
                ) {
                    Icon(
                        if (isActive && !paused) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
                InlineVoiceWaveform(
                    playing = isActive && !paused,
                    reducedMotion = reducedMotion,
                    modifier = Modifier.width(64.dp).height(24.dp)
                )
                Text(
                    if (isActive) formatInlineTime(positionMs) + " / " + formatInlineTime(shownDuration)
                    else formatInlineTime(fallbackDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                if (selected) {
                    IconButton(onClick = { onDeleteRequested(attachmentId) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete voice note",
                            tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    } else {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (selected) selected = false },
                    onLongClick = { selected = !selected }
                )
                .semantics { contentDescription = "Voice note: $displayName" },
            shape = RoundedCornerShape(18.dp),
            color = if (selected) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            // Surface content is a Box by default. Stack the header and seek row in a
            // Column so the slider never paints over the title, waveform, or timestamps.
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                FilledIconButton(
                    onClick = {
                        if (isActive) {
                            if (paused) {
                                runCatching { player?.resume() }.onSuccess { paused = false }
                            } else {
                                runCatching { player?.pause() }.onSuccess { paused = true }
                            }
                        } else {
                            onPlayRequested(attachmentId)
                        }
                    },
                    modifier = Modifier.size(44.dp).semantics {
                        contentDescription = when {
                            !isActive -> "Play $displayName"
                            paused -> "Resume $displayName"
                            else -> "Pause $displayName"
                        }
                    }
                ) {
                    Icon(
                        when {
                            isActive && !paused -> Icons.Default.Pause
                            else -> Icons.Default.PlayArrow
                        },
                        contentDescription = null
                    )
                }
    
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            displayName,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            if (isActive) {
                                formatInlineTime(positionMs) + " / " + formatInlineTime(shownDuration)
                            } else {
                                formatInlineTime(fallbackDurationMs)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    InlineVoiceWaveform(playing = isActive && !paused, reducedMotion = reducedMotion)
                }
    
                if (selected) {
                    IconButton(
                        onClick = { onDeleteRequested(attachmentId) },
                        modifier = Modifier.semantics { contentDescription = "Delete voice note" }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
                // Seeking must be available for short clips too; hiding the slider below
                // one minute made recordings under 60 seconds impossible to scrub.
                if (shownDuration > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 4.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        formatInlineTime(if (isActive) positionMs else 0),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Slider(
                        value = if (isActive) pendingSeekMs.coerceIn(0, shownDuration).toFloat() else 0f,
                        onValueChange = { value ->
                            if (isActive) {
                                seekingPlayback = true
                                pendingSeekMs = value.toInt()
                            }
                        },
                        onValueChangeFinished = {
                            if (isActive) {
                                positionMs = pendingSeekMs
                                runCatching { player?.seekTo(pendingSeekMs.toLong()) }
                            }
                            seekingPlayback = false
                        },
                        valueRange = 0f..shownDuration.coerceAtLeast(1).toFloat(),
                        enabled = isActive && shownDuration > 0,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary,
                            inactiveTrackColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.weight(1f).height(28.dp).semantics {
                            contentDescription = "Seek $displayName"
                            stateDescription = "${formatInlineTime(if (isActive) pendingSeekMs else 0)} of ${formatInlineTime(shownDuration)}"
                        }
                    )
                    Text(
                        formatInlineTime(shownDuration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                }
            }
        }
    }
    }

/**
 * Shown when a marker is present but the encrypted file for it is gone. The
 * most common cause is a manual clear of app storage or an interrupted sync;
 * the marker itself is dead weight, so offering a one-tap removal is safer
 * than hiding the problem.
 */
@Composable
private fun MissingVoiceAttachmentCard(
    attachmentId: String,
    onDeleteRequested: (attachmentId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Missing voice note attachment" },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    "Voice note unavailable",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    "The audio file is missing or was deleted.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
            }
            IconButton(
                onClick = { onDeleteRequested(attachmentId) },
                modifier = Modifier.semantics {
                    contentDescription = "Remove missing voice note marker"
                }
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

/** Three tiny bars marking the audio card that is currently playing. Decorative. */
@Composable
private fun InlinePlayingEqualizer(reducedMotion: Boolean) {
    val alpha = if (reducedMotion) {
        0.9f
    } else {
        val transition = rememberInfiniteTransition(label = "inline-equalizer")
        val pulsing by transition.animateFloat(
            initialValue = 0.25f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
            label = "inline-equalizer-alpha"
        )
        pulsing
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        listOf(12.dp, 18.dp, 8.dp).forEach { barHeight ->
            Box(
                Modifier
                    .width(3.dp)
                    .height(barHeight)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
    }
}


/** Minimal Material-themed waveform; bars animate only while playback is active. */
@Composable
private fun InlineVoiceWaveform(playing: Boolean, reducedMotion: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voice-waveform")
    val levels = listOf(0.28f, 0.62f, 0.42f, 0.9f, 0.5f, 0.74f, 0.34f, 0.82f, 0.46f, 0.68f, 0.3f, 0.58f)
    Row(
        modifier = modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        levels.forEachIndexed { index, level ->
            val animated by transition.animateFloat(
                initialValue = level * 0.55f,
                targetValue = level,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 360 + (index % 5) * 90, delayMillis = index * 23),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "voice-wave-$index"
            )
            val heightFraction = if (playing && !reducedMotion) animated else level * 0.72f
            Box(
                Modifier.weight(1f)
                    .height((20f * heightFraction.coerceIn(0.18f, 1f)).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(
                        alpha = if (playing) 0.95f else 0.48f + level * 0.25f
                    ))
            )
        }
    }
}

private fun formatInlineTime(durationMs: Int): String {
    val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}