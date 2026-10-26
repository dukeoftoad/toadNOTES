package com.nestnote.app

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun VoiceRecordingStatusPill(
    recording: Boolean, starting: Boolean, permissionPending: Boolean, paused: Boolean,
    elapsedSeconds: Int, reducedMotion: Boolean, onTogglePause: () -> Unit, onStop: () -> Unit
) {
    val minutes = elapsedSeconds / 60
    val seconds = elapsedSeconds % 60
    Surface(
        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RecordingStatusDot(active = recording && !paused, reducedMotion = reducedMotion)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(when {
                    starting || permissionPending -> "Preparing microphone…"
                    paused -> "Paused · %02d:%02d".format(minutes, seconds)
                    else -> "Recording · %02d:%02d".format(minutes, seconds)
                }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("Encrypted on this device · will attach at cursor", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
            }
            FilledIconButton(colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                contentColor = MaterialTheme.colorScheme.primaryContainer),
                enabled = recording, onClick = onTogglePause, modifier = Modifier.semantics {
                    contentDescription = if (paused) "Resume voice recording" else "Pause voice recording"
                    stateDescription = when {
                        !recording -> "Unavailable when not recording"
                        paused -> "Recording paused"
                        else -> "Recording in progress"
                    }
                }) { Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null) }
            FilledIconButton(colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                contentColor = MaterialTheme.colorScheme.primaryContainer),
                enabled = recording, onClick = onStop, modifier = Modifier.semantics {
                    contentDescription = "Stop voice recording"
                    stateDescription = if (recording) "Available" else "Unavailable when not recording"
                }) { Icon(Icons.Default.Stop, contentDescription = null) }
        }
    }
}

@Composable
internal fun VoiceRecordingIdleActions(importing: Boolean, onRecord: () -> Unit, onAttachWav: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(enabled = !importing, onClick = onRecord, modifier = Modifier.semantics {
            contentDescription = "Record voice note at cursor position"
        }) {
            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text("Record here")
        }
        OutlinedButton(enabled = !importing, onClick = onAttachWav, modifier = Modifier.semantics {
            contentDescription = "Attach WAV audio file at cursor position"
        }) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp)); Text(if (importing) "Importing…" else "Attach WAV")
        }
    }
}

@Composable
internal fun VoiceRecordingReviewCard(
    hasAttachment: Boolean, name: String, onNameChange: (String) -> Unit,
    durationMs: Int, positionMs: Int, seekPositionMs: Int, seeking: Boolean,
    previewing: Boolean, paused: Boolean, onSeekChange: (Int) -> Unit, onSeekFinished: () -> Unit,
    onTogglePreview: () -> Unit, onAttach: () -> Unit, onDiscard: () -> Unit
) {
    val duration = durationMs.coerceAtLeast(0)
    Card(Modifier.fillMaxWidth().semantics { contentDescription = "Recording review" },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Review recording", style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(if (previewing) "${reviewTime(positionMs)} / ${reviewTime(duration)}" else reviewTime(duration),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f))
            }
            OutlinedTextField(value = name, onValueChange = onNameChange, label = { Text("Recording name") },
                singleLine = true, modifier = Modifier.fillMaxWidth())
            if (duration > 0 && (previewing || positionMs > 0)) {
                Slider(value = (if (seeking) seekPositionMs else positionMs).coerceIn(0, duration).toFloat(),
                    onValueChange = { onSeekChange(it.toInt().coerceIn(0, duration)) },
                    onValueChangeFinished = onSeekFinished, enabled = previewing && hasAttachment,
                    valueRange = 0f..duration.toFloat(), modifier = Modifier.fillMaxWidth().semantics {
                        contentDescription = "Seek recording preview"
                        stateDescription = "${reviewTime(if (seeking) seekPositionMs else positionMs)} of ${reviewTime(duration)}"
                    })
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledIconButton(enabled = hasAttachment, onClick = onTogglePreview, modifier = Modifier.semantics {
                    contentDescription = if (previewing && !paused) "Pause preview recording" else "Preview recording"
                }) { Icon(if (previewing && !paused) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null) }
                Button(enabled = name.isNotBlank() && hasAttachment, onClick = onAttach, modifier = Modifier.weight(1f)) {
                    Text("Attach at cursor")
                }
                OutlinedButton(onClick = onDiscard) { Text("Discard") }
            }
        }
    }
}

@Composable
internal fun VoiceRecordingDiscardDialog(visible: Boolean, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    if (!visible) return
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Discard this recording?") },
        text = { Text("The recording will be permanently deleted and cannot be recovered.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Discard") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep recording") } })
}

private fun reviewTime(durationMs: Int): String {
    val seconds = (durationMs / 1000).coerceAtLeast(0)
    return "%d:%02d".format(seconds / 60, seconds % 60)
}

@Composable
private fun RecordingStatusDot(active: Boolean, reducedMotion: Boolean) {
    val alpha = if (active && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "recording-dot")
        val pulsing by transition.animateFloat(initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "recording-dot-alpha")
        pulsing
    } else 1f
    Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error.copy(alpha = alpha)))
}
