package com.nestnote.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun SettingsPrivacySection(
    screenCaptureProtectionEnabled: Boolean,
    onScreenCaptureProtectionChange: (Boolean) -> Unit,
    onOpenPrivateVaultSettings: () -> Unit,
    onOpenCallRecordings: () -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    SettingsSection(
        title = "Privacy",
        subtitle = "Your notes stay safely on your device",
        icon = Icons.Default.Lock
    ) {
        PrivacyFeatureRow(
            icon = Icons.Default.Security,
            title = "100% Local-first",
            body = "Notes are stored and encrypted on this device. They leave it only when you explicitly export or share them."
        )
    
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
    
        PrivacyFeatureRow(
            icon = Icons.Default.Lock,
            title = "No account or tracking",
            body = "No login required. Exporting does not automatically sync to any cloud server; you choose the file destination via Android's file picker."
        )
    
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
    
        SwitchRow(
            title = "Protect private screens",
            summary = if (screenCaptureProtectionEnabled)
                "Blocks screenshots and hides the app preview in Recents across NESTNOTE while enabled."
            else
                "Screenshots and app previews are allowed across NESTNOTE.",
            checked = screenCaptureProtectionEnabled,
            onChange = onScreenCaptureProtectionChange
        )
    
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
    
        OutlinedButton(
            onClick = onOpenPrivateVaultSettings,
            modifier = Modifier.fillMaxWidth().testTag("settings_private_vault"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Private Vault settings", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    
        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

        OutlinedButton(
            onClick = onOpenCallRecordings,
            modifier = Modifier.fillMaxWidth().testTag("settings_call_recordings"),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, colors.outlineVariant)
        ) {
            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Call recordings", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
        }

        HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

        Text(
            "Use Android app permissions and your chosen export destination to manage access to your data.",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(start = 2.dp)
        )
    }
}
