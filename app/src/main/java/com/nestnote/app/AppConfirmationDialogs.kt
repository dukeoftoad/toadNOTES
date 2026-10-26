package com.nestnote.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** Presentation-only confirmation; encryption, verification, and persistence stay with the app owner. */
@Composable
internal fun ConfirmEncryptAllDialog(
    eligibleCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Encrypt all ordinary notes?") },
        text = {
            Text(
                "This will encrypt and verify $eligibleCount ordinary notes using a dedicated device-bound Android Keystore key. " +
                    "Private Vault notes are excluded. If the device Keystore key is permanently lost, these notes may be unrecoverable. " +
                    "Continue only after considering that risk."
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Encrypt all") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** The caller owns the selected records and destructive storage operations. */
@Composable
internal fun ConfirmBulkDeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete $count notes?") },
        text = { Text("This permanently removes the selected notes and their voice recordings.") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Dialog presentation is extracted; draft and voice-discard behavior remain parent-owned. */
@Composable
internal fun ConfirmSystemDiscardDialog(
    isNewNote: Boolean,
    onDiscard: () -> Unit,
    onKeepEditing: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onKeepEditing,
        title = { Text("Discard changes?") },
        text = {
            Text(
                if (isNewNote) {
                    "Your unsaved note and any voice recordings attached to this new draft will be discarded."
                } else {
                    "Your unsaved changes and newly staged voice recordings will be discarded. Voice recordings already saved to this note will be kept."
                }
            )
        },
        confirmButton = { TextButton(onClick = onDiscard) { Text("Discard") } },
        dismissButton = { TextButton(onClick = onKeepEditing) { Text("Keep editing") } }
    )
}
