package com.nestnote.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

/**
 * The "Update" section of Settings: manual GitHub Releases check, in-app APK
 * download with progress, the "Install unknown apps" permission round-trip, and a
 * "View on GitHub" escape hatch. Ported from toadFILES' About update panel and
 * re-homed in this app's section-based Settings screen.
 *
 * All actions are explicit taps: check, download & install, open GitHub, dismiss.
 * The section arrives already seeded when the silent startup check (see
 * [StartupUpdateCheck]) found a newer release.
 */
@Composable
internal fun SettingsUpdateSection(modifier: Modifier = Modifier) {
    val updateViewModel: UpdateViewModel = viewModel()
    val update by updateViewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme

    SettingsSection(
        title = "Update",
        subtitle = "Check GitHub Releases for a newer toadNOTES",
        icon = Icons.Default.SystemUpdate,
        modifier = modifier
    ) {
        SettingLabel("Installed version")
        Text(
            BuildConfig.VERSION_NAME,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
        Text(
            "toadNOTES contacts GitHub Releases once per app open (silently, in the " +
                "background) and whenever you tap the button below. Nothing about this " +
                "device or your notes is sent, and nothing downloads without a tap.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        UpdatePanel(
            update = update,
            onCheck = updateViewModel::checkForUpdates,
            onDownloadInstall = { updateViewModel.downloadAndInstall(context) },
            onDismiss = updateViewModel::clearUpdateState
        )
    }
}

/**
 * The update panel proper — every [UpdateState] rendered in the section's existing
 * M3 style. All actions are explicit taps: check, download & install, open GitHub,
 * dismiss.
 */
@Composable
private fun UpdatePanel(
    update: UpdateState,
    onCheck: () -> Unit,
    onDownloadInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    when (update) {
        is UpdateState.Idle -> {
            Text(
                "The result of the silent startup check and your manual checks shows up here. " +
                    "No account, no telemetry, no periodic polling.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(
                onClick = onCheck,
                modifier = Modifier.testTag(SETTINGS_UPDATE_CHECK)
            ) { Text("Check for updates") }
        }

        is UpdateState.Checking -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Checking GitHub Releases…", style = MaterialTheme.typography.bodySmall)
            }
        }

        is UpdateState.UpToDate -> {
            Text(
                "You're up to date (${update.currentVersion}).",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            TextButton(onClick = onCheck) { Text("Check again") }
        }

        is UpdateState.Available -> UpdateAvailable(
            available = update,
            onDownloadInstall = onDownloadInstall,
            onDismiss = onDismiss
        )

        is UpdateState.Error -> {
            Text(
                update.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.testTag(SETTINGS_UPDATE_ERROR)
            )
            TextButton(onClick = onCheck) { Text("Retry") }
        }
    }
}

@Composable
private fun UpdateAvailable(
    available: UpdateState.Available,
    onDownloadInstall: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    var notesExpanded by rememberSaveable { mutableStateOf(false) }

    Text(
        "toadNOTES ${available.version} is available",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
    UpdateValueRow("New version", available.version)
    UpdateValueRow("Size", formatUpdateSize(available.sizeBytes))
    available.notes?.let { notes ->
        val lines = notes.split('\n')
        val truncated = !notesExpanded && lines.size > NOTES_PREVIEW_LINES
        Text(
            if (truncated) lines.take(NOTES_PREVIEW_LINES).joinToString("\n") + "\n…" else notes,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
        if (lines.size > NOTES_PREVIEW_LINES) {
            TextButton(onClick = { notesExpanded = !notesExpanded }) {
                Text(if (notesExpanded) "Show less" else "Show full notes")
            }
        }
    }
    if (available.needsInstallPermission) {
        Text(
            "Allow \"Install unknown apps\" for toadNOTES in the Android screen that just " +
                "opened, then tap Install again — the APK is already downloaded.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant
        )
    }
    if (available.downloadInProgress) {
        // Real in-app progress. Determinate when the release's total size is known
        // (GitHub always sends Content-Length), indeterminate otherwise — the bar
        // never lies. The byte counters come from the system DownloadManager's own
        // watch loop; the notification shade keeps mirroring the same job.
        val fraction = available.downloadProgress
        if (fraction != null) {
            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .testTag(SETTINGS_UPDATE_PROGRESS)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Downloading… ${(fraction * 100).toInt()}% — also visible in the notification shade.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .testTag(SETTINGS_UPDATE_PROGRESS)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Downloading… — also visible in the notification shade.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant
            )
        }
    }
    if (available.downloadError != null) {
        Text(
            available.downloadError, // smart-cast: immutable same-module val, null-checked above
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
            modifier = Modifier.testTag(SETTINGS_UPDATE_DOWNLOAD_ERROR)
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        val buttonLabel = when {
            available.downloadedUri != null -> "Install downloaded APK"
            available.downloadError != null -> "Retry download" // the failed attempt kept everything else
            else -> "Download & install"
        }
        Button(
            onClick = onDownloadInstall,
            enabled = !available.downloadInProgress,
            modifier = Modifier.testTag(SETTINGS_UPDATE_DOWNLOAD)
        ) {
            Text(buttonLabel)
        }
        Spacer(Modifier.width(8.dp))
        TextButton(onClick = { UpdateIntents.openUrl(context, available.htmlUrl) }) {
            Text("View on GitHub")
        }
    }
    TextButton(onClick = onDismiss) { Text("Dismiss") }
}

/** Label/value row in the same quiet style as the rest of the section. */
@Composable
private fun UpdateValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

/** Human-readable APK size; never pretends to know more than the payload said. */
private fun formatUpdateSize(bytes: Long): String = when {
    bytes >= 1024L * 1024 -> String.format(Locale.US, "%.1f MB", bytes / (1024f * 1024f))
    bytes >= 1024 -> String.format(Locale.US, "%.0f KB", bytes / 1024f)
    else -> "$bytes B"
}

internal const val SETTINGS_UPDATE_CHECK = "settings_update_check"
internal const val SETTINGS_UPDATE_PROGRESS = "settings_update_progress"
internal const val SETTINGS_UPDATE_ERROR = "settings_update_error"
internal const val SETTINGS_UPDATE_DOWNLOAD = "settings_update_download"
internal const val SETTINGS_UPDATE_DOWNLOAD_ERROR = "settings_update_download_error"

private const val NOTES_PREVIEW_LINES = 4
