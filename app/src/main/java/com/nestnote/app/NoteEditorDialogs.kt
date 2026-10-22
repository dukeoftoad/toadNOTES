@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.nestnote.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun NoteQrDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onSaveImage: (String) -> Unit
) {
    val qrPayload = remember(title, body) { NoteQrCode.sharePayload(title, body) }
    val qrBitmap = remember(qrPayload) {
        runCatching { NoteQrCode.createBitmap(qrPayload) }.getOrNull()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share with QR code") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                if (qrBitmap != null) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR code containing this note text. Scanning it reveals the note text.",
                        modifier = Modifier.size(220.dp)
                    )
                } else {
                    Text("This note is too long to fit in a single QR code. Shorten the note and try again.")
                }
                Text(
                    "Anyone who scans this code can read the note. The QR code contains the text directly; it is not an encrypted link.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
        dismissButton = {
            TextButton(enabled = qrBitmap != null, onClick = { onSaveImage(qrPayload) }) {
                Text("Save QR image")
            }
        }
    )
}

@Composable
internal fun DeleteNoteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete this note?") },
        text = {
            Text(
                "The note will be removed. You can undo deletion during the brief Undo " +
                    "window. Voice recordings are removed only after that window expires."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete note") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep note") } }
    )
}

@Composable
internal fun DiscardChangesDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Discard changes?") },
        text = { Text("Your unsaved changes will be lost.") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Discard") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep editing") } }
    )
}

@Composable
internal fun DraftRecoveryDialog(
    draft: DraftStore.Draft,
    timeFormatter: SimpleDateFormat,
    onRecover: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Recover unsaved changes?") },
        text = {
            Text(
                "A draft from ${timeFormatter.format(Date(draft.savedAtMillis))} " +
                    "was found. Recover it?"
            )
        },
        confirmButton = { TextButton(onClick = onRecover) { Text("Recover") } },
        dismissButton = { TextButton(onClick = onDiscard) { Text("Discard draft") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VoiceNoteSheet(
    noteId: Long,
    cursorAtRecordStart: () -> Int,
    addedThisSession: MutableSet<String>,
    onDismiss: () -> Unit,
    onRecordingAttached: (attachmentId: String, cursor: Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text("Voice note", style = MaterialTheme.typography.titleLarge)
            Text(
                "Recorded and stored encrypted on this device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp)
            )
            VoiceNoteControls(
                noteId = noteId,
                cursorAtRecordStart = cursorAtRecordStart,
                onRecordingAttached = onRecordingAttached,
                addedThisSession = addedThisSession
            )
        }
    }
}

@Composable
internal fun DeleteVoiceNoteDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete voice note?") },
        text = {
            Text(
                "The voice note will be removed from the note and its encrypted audio " +
                    "will be permanently deleted when you save. If you discard your " +
                    "changes instead, the voice note will be preserved."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Delete") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Keep") } }
    )
}
