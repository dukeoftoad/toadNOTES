package com.nestnote.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.Surface
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
    privateNoteCount: Int,
    onBiometricEnabledChange: (Boolean) -> Unit,
    onSetUpPassword: () -> Unit,
    onOpenVault: () -> Unit,
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
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn(tween(240)) + slideInVertically(tween(280)) { it / 12 }) {
            Card(
                modifier = Modifier.fillMaxWidth().animateContentSize(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = colors.primaryContainer)
            ) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Surface(shape = RoundedCornerShape(18.dp), color = colors.onPrimaryContainer.copy(alpha = 0.09f), modifier = Modifier.size(54.dp)) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.padding(13.dp).size(28.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Private Vault", style = MaterialTheme.typography.headlineSmall, color = colors.onPrimaryContainer)
                            Text("A quieter place for sensitive notes", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer.copy(alpha = 0.82f))
                        }
                    }
                    Text("Private notes stay out of your normal lists and search. Open your vault here or from Settings → Privacy.", style = MaterialTheme.typography.bodyMedium, color = colors.onPrimaryContainer)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = colors.onPrimaryContainer.copy(alpha = 0.10f)) {
                            Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(if (passwordConfigured) Icons.Default.CheckCircle else Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.onPrimaryContainer)
                                Text(if (passwordConfigured) "Password protected" else "Setup needed", style = MaterialTheme.typography.labelMedium, color = colors.onPrimaryContainer)
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Text("$privateNoteCount note${if (privateNoteCount == 1) "" else "s"}", style = MaterialTheme.typography.labelLarge, color = colors.onPrimaryContainer)
                    }
                    Button(onClick = if (passwordConfigured) onOpenVault else onSetUpPassword, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Icon(if (passwordConfigured) Icons.Default.Lock else Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(9.dp))
                        Text(if (passwordConfigured) "Open Private Vault" else "Set up vault password")
                    }
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn(tween(320, delayMillis = 60)) + slideInVertically(tween(320, delayMillis = 60)) { it / 14 }) {
            Card(modifier = Modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = colors.primaryContainer, modifier = Modifier.size(46.dp)) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = colors.onPrimaryContainer, modifier = Modifier.padding(10.dp).size(26.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Biometric unlock", style = MaterialTheme.typography.titleMedium)
                            Text("Use your fingerprint or supported strong biometric", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                        Switch(checked = biometricEnabled, onCheckedChange = onBiometricEnabledChange, enabled = passwordConfigured && biometricAvailable)
                    }
                    AnimatedVisibility(visible = biometricEnabled, enter = fadeIn(tween(180)) + slideInVertically(tween(220)) { -it / 5 }, exit = fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 5 }) {
                        Surface(shape = RoundedCornerShape(14.dp), color = colors.primary.copy(alpha = 0.08f)) {
                            Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = colors.primary, modifier = Modifier.size(19.dp))
                                Text("Biometric unlock is enabled on this device. Keep your vault password for recovery.", style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
                            }
                        }
                    }
                    AnimatedVisibility(visible = !biometricAvailable || !passwordConfigured, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
                        Text(if (!passwordConfigured) "Create a vault password before enrolling biometrics." else "Strong biometrics aren't currently available on this device. Password unlock still works.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    if (!biometricEnabled && biometricAvailable && passwordConfigured) {
                        Text("Enabling asks you to authenticate and securely enroll this device. Your biometric data never leaves Android's system.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    HorizontalDivider(color = colors.outlineVariant)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(14.dp), color = colors.secondaryContainer, modifier = Modifier.size(46.dp)) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = colors.onSecondaryContainer, modifier = Modifier.padding(11.dp).size(24.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Vault password", style = MaterialTheme.typography.titleMedium)
                            Text("Your reliable recovery method, even if biometrics become unavailable.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(onClick = {
                        currentPassword = ""
                        newPassword = ""
                        confirmation = ""
                        passwordError = ""
                        showPasswordDialog = true
                    }, enabled = passwordConfigured, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(15.dp)) {
                        Text("Change vault password")
                    }
                }
            }
        }

        AnimatedVisibility(visible = true, enter = fadeIn(tween(320, delayMillis = 120)) + slideInVertically(tween(320, delayMillis = 120)) { it / 16 }) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = colors.errorContainer.copy(alpha = 0.42f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                    Surface(shape = RoundedCornerShape(12.dp), color = colors.error.copy(alpha = 0.10f), modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = colors.error, modifier = Modifier.padding(9.dp).size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("Archive privacy", style = MaterialTheme.typography.titleSmall, color = colors.primary)
                        Text("Only password-protected encrypted archives are offered for portability. Private Vault notes are excluded from archives; keep a separate secure recovery plan for private content.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
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
                passwordError = ""
            },
            title = { Text("Change vault password") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = currentPassword, onValueChange = { currentPassword = it; passwordError = "" }, label = { Text("Current password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(value = newPassword, onValueChange = { newPassword = it; passwordError = "" }, label = { Text("New password (6+ characters)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    OutlinedTextField(value = confirmation, onValueChange = { confirmation = it; passwordError = "" }, label = { Text("Confirm new password") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp))
                    AnimatedVisibility(visible = passwordError.isNotBlank(), enter = fadeIn(tween(150)), exit = fadeOut(tween(100))) {
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
                    passwordError = ""
                }) { Text("Cancel") }
            }
        )
    }
}
