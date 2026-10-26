package com.nestnote.app

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Permission-denial actions, kept separate from recording and review state orchestration. */
@Composable
internal fun VoicePermissionSettingsActions(
    visible: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onStatus: (String) -> Unit
) {
    if (!visible) return
    val context = LocalContext.current

    TextButton(onClick = onRetry) { Text("Try again") }
    OutlinedButton(onClick = {
        onDismiss()
        runCatching {
            context.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:${context.packageName}")
                )
            )
        }.onFailure {
            onStatus("Open Android Settings and allow microphone access for NESTNOTE.")
        }
    }) { Text("Open app settings") }
}
