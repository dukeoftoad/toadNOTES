package com.nestnote.app

import android.content.Context
import android.content.ContextWrapper
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
    private const val PREF_FAILED_ATTEMPTS = "private_vault_failed_attempts"
    private const val PREF_RETRY_AT_MILLIS = "private_vault_retry_at_millis"
    private const val LOCKOUT_THRESHOLD = 5
    private const val INITIAL_LOCKOUT_MILLIS = 30_000L
    private const val MAX_LOCKOUT_MILLIS = 15 * 60_000L

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
                .remove(PREF_FAILED_ATTEMPTS)
                .remove(PREF_RETRY_AT_MILLIS)
                .commit()
        }.getOrDefault(false)
    }

    // Adapted security patterns: Markleaf's locked-notes screen applies FLAG_SECURE
    // to private content, and its passcode gate persists exponential retry backoff.
    // Source is Apache-2.0 licensed; attribution and license are in THIRD_PARTY_NOTICES.md.
    /** Persisted brute-force backoff so closing/restarting the app cannot reset it. */
    fun retryAtMillis(context: Context): Long =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(PREF_RETRY_AT_MILLIS, 0L)

    fun isLockedOut(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean =
        retryAtMillis(context) > nowMillis

    /**
     * Record a failed password attempt. After five failures, wait 30 seconds;
     * subsequent failures increase the wait exponentially, capped at 15 minutes.
     * Returns the retry deadline (0 when no delay is active).
     */
    fun recordFailedAttempt(context: Context, nowMillis: Long = System.currentTimeMillis()): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentDeadline = prefs.getLong(PREF_RETRY_AT_MILLIS, 0L)
        if (currentDeadline > nowMillis) return currentDeadline
        val attempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        val exponent = (attempts - LOCKOUT_THRESHOLD).coerceAtLeast(0).coerceAtMost(5)
        val delayMillis = if (attempts < LOCKOUT_THRESHOLD) 0L
            else (INITIAL_LOCKOUT_MILLIS * (1L shl exponent)).coerceAtMost(MAX_LOCKOUT_MILLIS)
        val deadline = if (delayMillis == 0L) 0L else nowMillis + delayMillis
        prefs.edit().putInt(PREF_FAILED_ATTEMPTS, attempts)
            .putLong(PREF_RETRY_AT_MILLIS, deadline).commit()
        return deadline
    }

    fun clearFailedAttempts(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(PREF_FAILED_ATTEMPTS)
            .remove(PREF_RETRY_AT_MILLIS)
            .commit()
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
    val activity = context.findFragmentActivity()
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
                    val retryAt = PrivateVaultSecurity.retryAtMillis(context)
                    error = when {
                        passwordConfigured && retryAt > System.currentTimeMillis() -> {
                            val seconds = ((retryAt - System.currentTimeMillis() + 999L) / 1_000L)
                            "Too many attempts. Try again in $seconds seconds."
                        }
                        passwordConfigured -> "Incorrect password."
                        else -> "Passwords must match and contain at least ${PrivateVaultSecurity.MIN_PASSWORD_LENGTH} characters."
                    }
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

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
