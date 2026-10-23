package com.nestnote.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricPrompt
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Optional biometric wrapping for the independently generated vault data key.
 *
 * Enrollment must be explicitly requested only after password authentication. The key is
 * authentication-bound and only a Cipher returned by BiometricPrompt may unwrap the persisted
 * data key. This class is deliberately not wired into the UI until device-level validation passes.
 */
internal class PrivateVaultBiometricKey(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Creates the auth-bound key and returns an encryption Cipher for CryptoObject enrollment. */
    fun createEnrollmentCipher(): Cipher? = runCatching {
        deleteEnrollment()
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, generateKey())
            updateAAD(AAD)
        }
    }.getOrNull()

    /** Persist only after BiometricPrompt successfully authenticates the enrollment Cipher. */
    fun completeEnrollment(session: PrivateNoteCrypto.Session, authenticatedCipher: Cipher?): Boolean =
        runCatching {
            val cipher = requireNotNull(authenticatedCipher) { "Missing authenticated enrollment cipher" }
            require(cipher.algorithm == TRANSFORMATION)
            val ciphertext = cipher.doFinal(session.key.encoded)
            val payload = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
                .put(cipher.iv).put(ciphertext).array()
            check(prefs.edit()
                .putInt(VERSION, SCHEMA_VERSION)
                .putString(WRAPPED_KEY, Base64.encodeToString(payload, Base64.NO_WRAP))
                .commit())
            true
        }.getOrDefault(false)

    /** Prepares the only Cipher that may be passed to BiometricPrompt.CryptoObject. */
    fun createUnlockCipher(): Cipher? = runCatching {
        val payload = wrappedPayload() ?: return null
        val iv = ByteArray(IV_BYTES)
        ByteBuffer.wrap(payload).get(iv)
        Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, existingKey(), GCMParameterSpec(TAG_BITS, iv))
            updateAAD(AAD)
        }
    }.getOrNull()

    /**
     * Must be called with result.cryptoObject?.cipher from a successful CryptoObject prompt.
     * A bare biometric success callback is insufficient to obtain the vault session.
     */
    fun unlock(authenticatedCipher: Cipher?): PrivateNoteCrypto.Session? = runCatching {
        requireNotNull(authenticatedCipher) { "Missing authenticated biometric cipher" }
        require(authenticatedCipher.algorithm == TRANSFORMATION)
        val payload = wrappedPayload() ?: return null
        val ciphertext = payload.copyOfRange(IV_BYTES, payload.size)
        val keyBytes = authenticatedCipher.doFinal(ciphertext)
        require(keyBytes.size == KEY_BYTES)
        PrivateNoteCrypto.Session(SecretKeySpec(keyBytes, "AES"))
    }.getOrNull()

    fun isEnrolled(): Boolean = prefs.getInt(VERSION, -1) == SCHEMA_VERSION &&
        !prefs.getString(WRAPPED_KEY, null).isNullOrBlank() &&
        runCatching { existingKey() }.isSuccess

    fun deleteEnrollment(): Boolean = runCatching {
        prefs.edit().remove(VERSION).remove(WRAPPED_KEY).commit().also {
            val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        }
    }.getOrDefault(false)

    private fun wrappedPayload(): ByteArray? {
        if (prefs.getInt(VERSION, -1) != SCHEMA_VERSION) return null
        return Base64.decode(prefs.getString(WRAPPED_KEY, null) ?: return null, Base64.NO_WRAP)
            .also { require(it.size >= IV_BYTES + TAG_BYTES) }
    }

    private fun generateKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (store.containsAlias(KEY_ALIAS)) store.deleteEntry(KEY_ALIAS)
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(KEY_BITS)
                    .setUserAuthenticationRequired(true)
                    .setInvalidatedByBiometricEnrollment(true)
                    .build()
            )
        }.generateKey()
    }

    private fun existingKey(): SecretKey {
        val store = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return store.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("Biometric vault key is unavailable")
    }

    companion object {
        private const val PREFS = "nestnote_private_vault_biometric"
        private const val VERSION = "schema_version"
        private const val WRAPPED_KEY = "wrapped_vault_key"
        private const val SCHEMA_VERSION = 1
        private const val KEY_ALIAS = "nestnote_private_vault_biometric_wrap_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_BITS = 256
        private const val KEY_BYTES = 32
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val TAG_BITS = 128
        private val AAD = "com.nestnote.app|private-vault|biometric-wrap|v1".toByteArray(Charsets.UTF_8)
    }
}
