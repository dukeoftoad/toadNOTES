package com.nestnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Persistent recovery warning; the caller owns recovery state and destination changes. */
@Composable
internal fun AppStorageRecoveryNotice(onOpenSettings: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            Modifier.padding(start = 14.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CloudOff, contentDescription = null, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text("Notes need recovery", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    "Stored notes couldn't be read. Nothing was overwritten. Restore a backup before creating, editing, or deleting notes.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            TextButton(onClick = onOpenSettings) { Text("Open") }
        }
    }
}

/** Recovery explanation only; no storage writes or restore operations are performed here. */
@Composable
internal fun AppStorageRecoveryDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Local notes need recovery") },
        text = {
            Text("NESTNOTE couldn't read the saved notes. The stored data has been left untouched. Open Settings and restore a backup before creating, editing, or deleting notes.")
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) { Text("Open backup settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        }
    )
}
