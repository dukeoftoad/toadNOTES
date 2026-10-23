package com.nestnote.app

import android.content.Context
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object PrivateVaultSecurity {
    const val MIN_PASSWORD_LENGTH = 6
    private const val ITERATIONS = 160_000
    private const val KEY_BITS = 256

    fun isConfigured(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return !prefs.getString(PREF_PRIVATE_VAULT_SALT, null).isNullOrBlank() &&
            !prefs.getString(PREF_PRIVATE_VAULT_HASH, null).isNullOrBlank()
    }

    fun configure(context: Context, password: String): Boolean {
        if (password.length < MIN_PASSWORD_LENGTH) return false
        return runCatching {
            val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val hash = derive(password, salt)
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putString(PREF_PRIVATE_VAULT_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(PREF_PRIVATE_VAULT_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .commit()
        }.getOrDefault(false)
    }

    fun verify(context: Context, password: String): Boolean = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val salt = Base64.decode(prefs.getString(PREF_PRIVATE_VAULT_SALT, null), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(PREF_PRIVATE_VAULT_HASH, null), Base64.NO_WRAP)
        MessageDigest.isEqual(expected, derive(password, salt))
    }.getOrDefault(false)

    fun changePassword(context: Context, current: String, newPassword: String, confirmation: String): Boolean {
        if (!isConfigured(context) || !verify(context, current)) return false
        if (newPassword.length < MIN_PASSWORD_LENGTH || newPassword != confirmation) return false
        return configure(context, newPassword)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally { spec.clearPassword() }
    }
}

@Composable
internal fun PrivateVaultGateDialog(
    passwordConfigured: Boolean,
    biometricEnabled: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (password: String, confirmation: String) -> Boolean,
    onBiometricUnlock: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember(activity, passwordConfigured, biometricEnabled) {
        passwordConfigured && biometricEnabled && activity != null &&
            BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    fun launchBiometricPrompt() {
        val host = activity ?: return
        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    error = ""
                    onBiometricUnlock()
                }

                override fun onAuthenticationFailed() {
                    error = "Fingerprint not recognized. Try again or use your password."
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancellation is not an unlock; keep the password fallback available.
                    error = if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) "" else errString.toString()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Private Vault")
            .setSubtitle("Confirm your identity with your fingerprint or device biometric")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
            .setNegativeButtonText("Use password")
            .build()
        prompt.authenticate(promptInfo)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (passwordConfigured) "Unlock Private Vault" else "Set up Private Vault") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (passwordConfigured) {
                        "Enter your vault password to reveal hidden notes, or use a registered device biometric."
                    } else {
                        "Choose a password with at least ${PrivateVaultSecurity.MIN_PASSWORD_LENGTH} characters. This password cannot be recovered if forgotten."
                    }
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
                if (!passwordConfigured) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                    )
                }
                if (error.isNotBlank()) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (!onSubmit(password, confirmation)) {
                    error = if (passwordConfigured) "Incorrect password."
                    else "Passwords must match and contain at least ${PrivateVaultSecurity.MIN_PASSWORD_LENGTH} characters."
                }
            }) { Text(if (passwordConfigured) "Unlock with password" else "Create password") }
        },
        dismissButton = {
            Column {
                if (biometricAvailable) {
                    TextButton(onClick = ::launchBiometricPrompt) {
                        Text("Unlock with fingerprint")
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
