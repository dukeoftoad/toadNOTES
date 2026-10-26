package com.nestnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun BackupRestoreScreen(
    totalNotes: Int,
    archivedNotes: Int,
    pinnedNotes: Int,
    lastBackupAt: Long,
    backupStatus: String,
    onChooseBackupFolder: () -> Unit,
    backupFolderConfigured: Boolean,
    onExportEncryptedArchive: (Boolean, String) -> Unit,
    onImportEncryptedArchive: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    var includeArchivedInBackup by rememberSaveable { mutableStateOf(true) }
    var archivePassword by rememberSaveable { mutableStateOf("") }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
            border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.6f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Encrypted note archive", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Move notes between devices using password-protected encrypted archives.",
                    style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    BackupStatTile("Active", (totalNotes - archivedNotes).coerceAtLeast(0), Icons.Default.Edit, Modifier.weight(1f))
                    BackupStatTile("Archived", archivedNotes, Icons.Default.Archive, Modifier.weight(1f))
                    BackupStatTile("Pinned", pinnedNotes, Icons.Default.PushPin, Modifier.weight(1f))
                }
            }
        }

        SettingsSection(
            title = "Archive location",
            subtitle = "Choose where encrypted archives are stored",
            icon = Icons.Default.FolderOpen
        ) {
            Text(
                if (backupFolderConfigured) "Folder access is saved. Encrypted archives will be stored in Documents/NESTNOTE."
                else "Choose a folder once and grant NESTNOTE access to store encrypted archives.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
            )
            OutlinedButton(onClick = onChooseBackupFolder, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (backupFolderConfigured) "Change archive folder" else "Choose archive folder")
            }
        }

        SettingsSection(
            title = "Archive status",
            subtitle = "Last encrypted archive operation",
            icon = Icons.Default.Security
        ) {
            Text(
                if (lastBackupAt > 0L) "Last operation: " + SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault()).format(Date(lastBackupAt)) + " · $backupStatus"
                else "No encrypted archive has been created in this installation.",
                style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant
            )
        }

        SettingsSection(
            title = "Password-protected archive",
            subtitle = "AES-GCM encrypted .nestnote file",
            icon = Icons.Default.Lock
        ) {
            Text(
                "Use a strong password of at least 10 characters. Keep it separately; NESTNOTE cannot recover a forgotten archive password. Voice recordings are not included.",
                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant
            )
            SwitchRow(
                title = "Include archived notes",
                summary = "Turn off to archive active notes only",
                checked = includeArchivedInBackup,
                onChange = { includeArchivedInBackup = it }
            )
            OutlinedTextField(
                value = archivePassword,
                onValueChange = { archivePassword = it.take(256) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Archive password (10+ characters)") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
            OutlinedButton(
                onClick = {
                    val password = archivePassword
                    archivePassword = ""
                    onExportEncryptedArchive(includeArchivedInBackup, password)
                },
                enabled = backupFolderConfigured && archivePassword.length >= 10,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Create encrypted archive")
            }
            OutlinedButton(
                onClick = {
                    val password = archivePassword
                    archivePassword = ""
                    onImportEncryptedArchive(password)
                },
                enabled = backupFolderConfigured && archivePassword.length >= 10,
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Restore encrypted archive")
            }
        }
    }
}

@Composable
private fun BackupStatTile(
    label: String,
    count: Int,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceContainerHighest.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = colors.primary, modifier = Modifier.size(20.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
        }
    }
}
