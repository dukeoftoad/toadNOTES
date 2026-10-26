package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Presentation-only recovery prompt; draft persistence and recovery remain parent-owned. */
@Composable
internal fun OrphanDraftRecoveryDialog(
    count: Int,
    preview: String,
    savedAtLabel: String,
    onDismiss: () -> Unit,
    onRecover: () -> Unit,
    onDiscardAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Recover unsaved note?" else "Recover unsaved notes?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (count == 1) "A draft from $savedAtLabel was found."
                    else "$count drafts from previous sessions were found. The most recent is:"
                )
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(preview, style = MaterialTheme.typography.titleSmall)
                        Text(savedAtLabel, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onRecover) { Text("Recover") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                TextButton(onClick = onDiscardAll) { Text("Discard all") }
                TextButton(onClick = onDismiss) { Text("Later") }
            }
        }
    )
}

/** The caller calculates and persists the restore preview; this component only presents confirmation. */
@Composable
internal fun BackupRestoreReviewDialog(
    addedCount: Int,
    updatedCount: Int,
    withinNoteLimit: Boolean,
    maxNotes: Int,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review backup restore") },
        text = {
            Text(
                if (withinNoteLimit) {
                    "This backup will add $addedCount notes and update $updatedCount existing notes. Voice recordings are not included. Notes not present in the backup will not be deleted."
                } else {
                    "This restore would exceed NESTNOTE's $maxNotes-note limit. Remove notes or choose a smaller backup before restoring."
                }
            )
        },
        confirmButton = {
            TextButton(enabled = withinNoteLimit, onClick = onRestore) { Text("Restore notes") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
