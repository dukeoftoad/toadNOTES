package com.nestnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsDataSection(
    totalNotes: Int,
    archivedNotes: Int,
    onOpenBackupRestore: () -> Unit,
    storageBackend: NoteStorageBackend,
    onRequestStorageBackendChange: (NoteStorageBackend) -> Unit
) {
    val colors = MaterialTheme.colorScheme
    SettingsSection(
        title = "Data",
        subtitle = "Manage encrypted archives and local storage",
        icon = Icons.Default.Archive
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OverviewTile(
                label = "Active notes",
                count = (totalNotes - archivedNotes).coerceAtLeast(0),
                onClick = onOpenBackupRestore,
                modifier = Modifier.weight(1f)
            )
            OverviewTile(
                label = "Archived notes",
                count = archivedNotes,
                onClick = onOpenBackupRestore,
                modifier = Modifier.weight(1f)
            )
        }
    
        Text(
            "Create or restore a password-protected encrypted archive.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    
        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
        SettingLabel("Encrypted note storage")
        Text(
            "Choose which private app-data store NESTNOTE uses. Switching never deletes the other store; a new SQLCipher store is initialized from the current notes only the first time.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable { if (storageBackend != NoteStorageBackend.KEYSTORE) onRequestStorageBackendChange(NoteStorageBackend.KEYSTORE) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = storageBackend == NoteStorageBackend.KEYSTORE,
                    onClick = { if (storageBackend != NoteStorageBackend.KEYSTORE) onRequestStorageBackendChange(NoteStorageBackend.KEYSTORE) }
                )
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Android Keystore encrypted storage", style = MaterialTheme.typography.titleSmall)
                    Text("AES-GCM encrypted note payload in app preferences", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .clickable { if (storageBackend != NoteStorageBackend.SQLCIPHER) onRequestStorageBackendChange(NoteStorageBackend.SQLCIPHER) }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = storageBackend == NoteStorageBackend.SQLCIPHER,
                    onClick = { if (storageBackend != NoteStorageBackend.SQLCIPHER) onRequestStorageBackendChange(NoteStorageBackend.SQLCIPHER) }
                )
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Text("Encrypted SQL database (SQLCipher)", style = MaterialTheme.typography.titleSmall)
                    Text("Database under app-private storage; its key is wrapped by Android Keystore", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    
        // Opens the encrypted archive workflow.
        OutlinedButton(
            onClick = onOpenBackupRestore,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_open_backup_restore"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Encrypted archive", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
