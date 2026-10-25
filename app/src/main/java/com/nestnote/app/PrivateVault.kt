package com.nestnote.app

import android.content.Context
import android.content.ContextWrapper
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.crypto.Cipher
import kotlinx.coroutines.launch

internal object PrivateVaultSecurity {
    const val MIN_PASSWORD_LENGTH = 6
    private const val ITERATIONS = 160_000
    private const val KEY_BITS = 256
    private const val PREF_FAILED_ATTEMPTS = "private_vault_failed_attempts"
    private const val PREF_RETRY_AT_MILLIS = "private_vault_retry_at_millis"
    private const val PREF_RETRY_BOOT_COUNT = "private_vault_retry_boot_count"
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
                .remove(PREF_RETRY_BOOT_COUNT)
                .commit()
        }.getOrDefault(false)
    }

    // Adapted security patterns: Markleaf's locked-notes screen applies FLAG_SECURE
    // to private content, and its passcode gate persists exponential retry backoff.
    // Source is Apache-2.0 licensed; attribution and license are in THIRD_PARTY_NOTICES.md.
    private fun currentBootCount(context: Context): Int =
        runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT) }
            .getOrDefault(-1)

    /**
     * Lockout deadlines use monotonic elapsed time, not the user-adjustable wall clock.
     * If Android reports a reboot while a cooldown is active, require a fresh bounded
     * cooldown rather than trusting a deadline from the previous boot's time base.
     */
    fun retryAtMillis(
        context: Context,
        nowMillis: Long = SystemClock.elapsedRealtime(),
        bootCount: Int = currentBootCount(context)
    ): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val deadline = prefs.getLong(PREF_RETRY_AT_MILLIS, 0L)
        if (deadline <= 0L) return 0L
        val savedBootCount = prefs.getInt(PREF_RETRY_BOOT_COUNT, bootCount)
        if (bootCount >= 0 && savedBootCount >= 0 && savedBootCount != bootCount) {
            val rebootDeadline = nowMillis + MAX_LOCKOUT_MILLIS
            prefs.edit().putLong(PREF_RETRY_AT_MILLIS, rebootDeadline)
                .putInt(PREF_RETRY_BOOT_COUNT, bootCount).commit()
            return rebootDeadline
        }
        return deadline
    }

    fun isLockedOut(
        context: Context,
        nowMillis: Long = SystemClock.elapsedRealtime(),
        bootCount: Int = currentBootCount(context)
    ): Boolean = retryAtMillis(context, nowMillis, bootCount) > nowMillis

    /**
     * Record a failed password attempt. After five failures, wait 30 seconds;
     * subsequent failures increase the wait exponentially, capped at 15 minutes.
     * Returns the monotonic retry deadline (0 when no delay is active).
     */
    fun recordFailedAttempt(
        context: Context,
        nowMillis: Long = SystemClock.elapsedRealtime(),
        bootCount: Int = currentBootCount(context)
    ): Long {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val currentDeadline = retryAtMillis(context, nowMillis, bootCount)
        if (currentDeadline > nowMillis) return currentDeadline
        val attempts = prefs.getInt(PREF_FAILED_ATTEMPTS, 0) + 1
        val exponent = (attempts - LOCKOUT_THRESHOLD).coerceAtLeast(0).coerceAtMost(5)
        val delayMillis = if (attempts < LOCKOUT_THRESHOLD) 0L
            else (INITIAL_LOCKOUT_MILLIS * (1L shl exponent)).coerceAtMost(MAX_LOCKOUT_MILLIS)
        val deadline = if (delayMillis == 0L) 0L else nowMillis + delayMillis
        prefs.edit().putInt(PREF_FAILED_ATTEMPTS, attempts)
            .putLong(PREF_RETRY_AT_MILLIS, deadline)
            .putInt(PREF_RETRY_BOOT_COUNT, bootCount).commit()
        return deadline
    }

    fun clearFailedAttempts(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(PREF_FAILED_ATTEMPTS)
            .remove(PREF_RETRY_AT_MILLIS)
            .remove(PREF_RETRY_BOOT_COUNT)
            .commit()
    }

    fun verify(context: Context, password: String): Boolean = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val salt = Base64.decode(prefs.getString(PREF_PRIVATE_VAULT_SALT, null), Base64.NO_WRAP)
        val expected = Base64.decode(prefs.getString(PREF_PRIVATE_VAULT_HASH, null), Base64.NO_WRAP)
        MessageDigest.isEqual(expected, derive(password, salt))
    }.getOrDefault(false)

    fun changePassword(context: Context, current: String, newPassword: String, confirmation: String): Boolean {
        if (!isConfigured(context) || isLockedOut(context)) return false
        if (!verify(context, current)) {
            recordFailedAttempt(context)
            return false
        }
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
    onSubmit: (password: String, confirmation: String, onResult: (Boolean) -> Unit) -> Unit,
    onBiometricUnlock: (Cipher, (Boolean) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    val biometricAvailable = remember(activity, passwordConfigured, biometricEnabled) {
        passwordConfigured && biometricEnabled && activity != null &&
            BiometricManager.from(activity).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    fun launchBiometricPrompt() {
        val host = activity ?: return
        val authenticatedCipher = PrivateVaultBiometricKey(context).createUnlockCipher()
        if (authenticatedCipher == null) {
            error = "Biometric unlock is not enrolled or the device key is unavailable. Use your password."
            return
        }
        val prompt = BiometricPrompt(
            host,
            ContextCompat.getMainExecutor(host),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    val cipher = result.cryptoObject?.cipher
                    if (cipher == null) {
                        error = "Biometric authentication did not authorize the vault key. Use your password."
                    } else {
                        error = ""
                        submitting = true
                        onBiometricUnlock(cipher) { unlocked ->
                            submitting = false
                            if (!unlocked) error = "Vault could not be opened safely. Use your password."
                        }
                    }
                }

                override fun onAuthenticationFailed() {
                    error = "Fingerprint not recognized. Try again or use your password."
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // Cancellation is not an unlock; keep the password fallback available.
                    submitting = false
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
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("Use password")
            .build()
        prompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(authenticatedCipher))
    }

    // Prefer fingerprint on entry when this device has a valid enrollment, while leaving
    // password unlock immediately available if the user cancels or biometric auth fails.
    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable && PrivateVaultBiometricKey(context).isEnrolled()) {
            launchBiometricPrompt()
        }
    }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        if (passwordConfigured) Icons.Default.Lock else Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp).size(24.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(if (passwordConfigured) "Welcome back" else "Secure your private notes")
                    Text(if (passwordConfigured) "PRIVATE VAULT" else "FIRST-TIME SETUP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (passwordConfigured) {
                        "Enter your vault password to reveal hidden notes, or use an enrolled strong biometric."
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
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                        }
                    },
                    enabled = !submitting,
                    shape = RoundedCornerShape(14.dp)
                )
                if (!passwordConfigured) {
                    OutlinedTextField(
                        value = confirmation,
                        onValueChange = { confirmation = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        enabled = !submitting,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
                AnimatedVisibility(visible = error.isNotBlank(), enter = fadeIn(tween(160)) + slideInVertically(tween(180)) { -it / 4 }, exit = fadeOut(tween(100))) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !submitting, onClick = {
                if (submitting) return@TextButton
                submitting = true
                error = ""
                onSubmit(password, confirmation) { accepted ->
                    submitting = false
                    if (!accepted) {
                        val retryAt = PrivateVaultSecurity.retryAtMillis(context)
                        error = when {
                            passwordConfigured && retryAt > SystemClock.elapsedRealtime() -> {
                                val seconds = ((retryAt - SystemClock.elapsedRealtime() + 999L) / 1_000L)
                                "Too many attempts. Try again in $seconds seconds."
                            }
                            passwordConfigured -> "Incorrect password."
                            else -> "Passwords must match and contain at least ${PrivateVaultSecurity.MIN_PASSWORD_LENGTH} characters."
                        }
                    }
                }
            }) {
                if (submitting) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Unlocking…")
                    }
                } else Text(if (passwordConfigured) "Unlock vault" else "Create vault")
            }
        },
        dismissButton = {
            Column {
                if (biometricAvailable && PrivateVaultBiometricKey(context).isEnrolled()) {
                    TextButton(enabled = !submitting, onClick = ::launchBiometricPrompt) {
                        Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(7.dp))
                        Text("Unlock with fingerprint")
                    }
                }
                TextButton(enabled = !submitting, onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}


/**
 * Explicit enrollment flow. The vault must already be unlocked with the password before this
 * dialog is shown; only the authenticated CryptoObject cipher can wrap the vault data key.
 */
@Composable
internal fun PrivateVaultBiometricEnrollmentDialog(
    session: PrivateNoteCrypto.Session,
    onComplete: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findFragmentActivity()
    var message by remember { mutableStateOf("") }
    var promptLaunched by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(10.dp).size(24.dp))
                }
                Text("Enable biometric unlock")
            }
        },
        text = {
            Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Confirm your fingerprint or strong device biometric to protect the vault key. Your vault password will remain available as a recovery method.")
                AnimatedVisibility(visible = message.isNotBlank(), enter = fadeIn(tween(160)) + slideInVertically(tween(180)) { -it / 4 }, exit = fadeOut(tween(100))) {
                    Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = activity != null && !promptLaunched,
                onClick = {
                    val host = activity ?: return@TextButton
                    val biometric = PrivateVaultBiometricKey(context)
                    val cipher = biometric.createEnrollmentCipher()
                    if (cipher == null) {
                        message = "Couldn't prepare a secure biometric key. Your vault remains password-protected."
                        return@TextButton
                    }
                    promptLaunched = true
                    val prompt = BiometricPrompt(
                        host,
                        ContextCompat.getMainExecutor(host),
                        object : BiometricPrompt.AuthenticationCallback() {
                            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                                val authenticatedCipher = result.cryptoObject?.cipher
                                val enrolled = authenticatedCipher != null &&
                                    biometric.completeEnrollment(session, authenticatedCipher)
                                if (enrolled) onComplete(true) else {
                                    val reason = if (authenticatedCipher == null) {
                                        "Android did not return the authenticated encryption cipher."
                                    } else {
                                        biometric.lastEnrollmentFailure ?: "The secure enrollment step did not complete."
                                    }
                                    biometric.deleteEnrollment()
                                    promptLaunched = false
                                    message = "$reason Biometric unlock was not enabled; use your vault password."
                                }
                            }

                            override fun onAuthenticationFailed() {
                                message = "Biometric not recognized. Try again."
                            }

                            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                                promptLaunched = false
                                message = if (
                                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                                    errorCode == BiometricPrompt.ERROR_CANCELED
                                ) "Enrollment cancelled. Your vault remains password-protected."
                                else errString.toString()
                            }
                        }
                    )
                    val info = BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Enable Private Vault biometric unlock")
                        .setSubtitle("Authenticate to securely wrap the vault key")
                        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                        .setNegativeButtonText("Cancel")
                        .build()
                    prompt.authenticate(info, BiometricPrompt.CryptoObject(cipher))
                }
            ) { Text("Authenticate and enable") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Not now") }
        }
    )
}
