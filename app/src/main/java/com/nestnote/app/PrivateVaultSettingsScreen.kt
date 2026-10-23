package com.nestnote.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun PrivateVaultSettingsScreen(
    passwordConfigured: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onSetUpPassword: () -> Unit,
    onChangePassword: (current: String, new: String, confirmation: String) -> Boolean
) {
    var showPasswordDialog by rememberSaveable { mutableStateOf(false) }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var passwordError by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Security, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.size(30.dp))
                Text("Your private space", style = MaterialTheme.typography.headlineSmall, color = colors.onPrimaryContainer)
                Text(
                    "Keep selected notes out of normal lists and search. Open the vault by swiping down on the Home title area, then authenticate.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onPrimaryContainer
                )
                Text(
                    if (passwordConfigured) "Password protection is configured" else "Finish setup by creating a vault password",
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onPrimaryContainer
                )
                if (!passwordConfigured) {
                    Button(onClick = onSetUpPassword, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text("Set up vault password")
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = colors.primary, modifier = Modifier.size(26.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Fingerprint / biometric unlock", style = MaterialTheme.typography.titleMedium)
                        Text(
                            when {
                                !passwordConfigured -> "Set a vault password first"
                                !biometricAvailable -> "No supported biometric is currently enrolled"
                                else -> "Use Android's system prompt for faster access"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = biometricEnabled,
                        onCheckedChange = onBiometricEnabledChange,
                        enabled = passwordConfigured && biometricAvailable
                    )
                }
                HorizontalDivider(color = colors.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Vault password", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Change it any time by confirming the current password first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
                OutlinedButton(
                    onClick = {
                        currentPassword = ""
                        newPassword = ""
                        confirmation = ""
                        passwordError = ""
                        showPasswordDialog = true
                    },
                    enabled = passwordConfigured,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Change vault password") }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.SwipeDown, contentDescription = null, tint = colors.primary)
                    Text("Quick access", style = MaterialTheme.typography.titleMedium)
                }
                Text("From Home, swipe down on the NESTNOTE title area. The vault locks when the app leaves the foreground; lock it manually with the lock icon before handing your phone to someone else.", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = colors.errorContainer.copy(alpha = 0.55f))
        ) {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.error)
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Backup privacy", style = MaterialTheme.typography.titleSmall, color = colors.error)
                    Text("The vault password controls access in the app; it is not a separate encryption key. Exported JSON backups can contain private notes in readable form. Store exports securely.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = {
                showPasswordDialog = false
                currentPassword = ""
                newPassword = ""
                confirmation = ""
            },
            title = { Text("Change vault password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; passwordError = "" },
                        label = { Text("Current password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordError = "" },
                        label = { Text("New password (6+ characters)") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it; passwordError = "" },
                        label = { Text("Confirm new password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (passwordError.isNotBlank()) {
                        Text(passwordError, color = colors.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (onChangePassword(currentPassword, newPassword, confirmation)) {
                        showPasswordDialog = false
                        currentPassword = ""
                        newPassword = ""
                        confirmation = ""
                        passwordError = ""
                    } else {
                        passwordError = "Check the current password and ensure the new passwords match and contain at least 6 characters."
                    }
                }) { Text("Update password") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPasswordDialog = false
                    currentPassword = ""
                    newPassword = ""
                    confirmation = ""
                }) { Text("Cancel") }
            }
        )
    }
}
