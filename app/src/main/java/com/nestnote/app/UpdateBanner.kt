package com.nestnote.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * The subtle, dismissible "update available" card shown once on app open when the
 * silent startup check found a newer release. Ported from toadFILES
 * (com.toadfiles.app.update.UpdateBanner). Non-intrusive by design: a single quiet
 * line above the notes list, no dialog, no sound, nothing downloads until the user
 * acts. "Update" deep-links into the EXISTING flow — it opens Settings, whose
 * update panel arrives already seeded with this release (see [UpdateViewModel]),
 * so the familiar "Download & install" controls are one tap away.
 */
@Composable
fun UpdateBanner(
    release: GitHubRelease?,
    onOpenUpdateFlow: () -> Unit,
    onDismiss: () -> Unit,
) {
    if (release == null) return // nothing to say — stays fully invisible
    Card(
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    "toadNOTES ${release.tagName} available",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    "Update to the latest release — downloaded only if you ask.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = onOpenUpdateFlow) { Text("Update") }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss update banner")
            }
        }
    }
}
