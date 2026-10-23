package com.nestnote.app

import android.content.Context
import android.util.Base64
import java.nio.ByteBuffer
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.KeyGenerator
import javax.crypto.spec.PBEKeySpec

/**
 * Independent encryption primitive for Private Vault records.
 *
 * A random vault data key encrypts records and is stored only as an AES-GCM wrapped value
 * under a PBKDF2 password-derived key. The ordinary NoteStore key cannot decrypt these records.
 * Session objects must be dropped when the vault locks. Biometric unlock must not bypass this
 * password-wrapped key unless an authentication-bound Keystore wrapping path is explicitly enrolled.
 */
internal class PrivateNoteCrypto(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    internal class Session internal constructor(internal val key: SecretKey) {
        fun encrypt(plaintext: ByteArray, recordId: Long): String {
            require(recordId > 0)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key)
            cipher.updateAAD(recordAad(recordId))
            val encrypted = cipher.doFinal(plaintext)
            return RECORD_PREFIX + Base64.encodeToString(
                ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array(),
                Base64.NO_WRAP
            )
        }

        fun decrypt(envelope: String, recordId: Long): ByteArray {
            require(recordId > 0 && envelope.startsWith(RECORD_PREFIX))
            val payload = Base64.decode(envelope.removePrefix(RECORD_PREFIX), Base64.NO_WRAP)
            require(payload.size >= IV_BYTES + TAG_BYTES)
            val b = ByteBuffer.wrap(payload)
            val iv = ByteArray(IV_BYTES); b.get(iv)
            val encrypted = ByteArray(b.remaining()); b.get(encrypted)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
            cipher.updateAAD(recordAad(recordId))
            return cipher.doFinal(encrypted)
        }
    }

    /** Creates the independent key once. Never overwrites existing key material on setup. */
    fun initialize(password: CharArray): Boolean = runCatching {
        require(password.size >= MIN_PASSWORD_LENGTH)
        synchronized(LOCK) {
            if (prefs.contains(WRAPPED_KEY)) {
                // Retrying a partially completed first unlock is safe only when the supplied
                // password actually unwraps the existing key. Never replace existing material.
                unlock(password) != null
            } else {
                val vaultKey = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
                val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
                val wrappingKey = derive(password, salt)
                val wrapped = wrap(vaultKey, wrappingKey)
                check(prefs.edit()
                    .putInt(VERSION, SCHEMA_VERSION)
                    .putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                    .putString(WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                    .commit()) { "Unable to persist private vault key metadata" }
                true
            }
        }
    }.getOrDefault(false)

    /** Wrong passwords, damaged metadata, or GCM tag failures produce no session. */
    fun unlock(password: CharArray): Session? = runCatching {
        synchronized(LOCK) {
            if (prefs.getInt(VERSION, -1) != SCHEMA_VERSION) return null
            val salt = Base64.decode(prefs.getString(SALT, null) ?: return null, Base64.NO_WRAP)
            require(salt.size == SALT_BYTES)
            val wrapped = Base64.decode(prefs.getString(WRAPPED_KEY, null) ?: return null, Base64.NO_WRAP)
            Session(unwrap(wrapped, derive(password, salt)))
        }
    }.getOrNull()

    /** Rewraps the same data key; stored private records need not be re-encrypted. */
    fun changePassword(current: CharArray, replacement: CharArray): Boolean = runCatching {
        require(replacement.size >= MIN_PASSWORD_LENGTH)
        synchronized(LOCK) {
            val session = unlock(current) ?: return false
            val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
            val wrapped = wrap(session.key, derive(replacement, salt))
            prefs.edit().putString(SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .putInt(VERSION, SCHEMA_VERSION).commit()
        }
    }.getOrDefault(false)

    fun isInitialized() = prefs.getInt(VERSION, -1) == SCHEMA_VERSION &&
        !prefs.getString(SALT, null).isNullOrBlank() &&
        !prefs.getString(WRAPPED_KEY, null).isNullOrBlank()

    private fun derive(password: CharArray, salt: ByteArray): SecretKey {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded, "AES")
        } finally { spec.clearPassword() }
    }

    private fun wrap(key: SecretKey, wrappingKey: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey); cipher.updateAAD(WRAP_AAD)
        val encrypted = cipher.doFinal(key.encoded)
        return ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array()
    }

    private fun unwrap(payload: ByteArray, wrappingKey: SecretKey): SecretKey {
        require(payload.size >= IV_BYTES + TAG_BYTES)
        val b = ByteBuffer.wrap(payload)
        val iv = ByteArray(IV_BYTES); b.get(iv)
        val encrypted = ByteArray(b.remaining()); b.get(encrypted)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(WRAP_AAD)
        return SecretKeySpec(cipher.doFinal(encrypted), "AES")
    }

    companion object {
        private const val PREFS = "nestnote_private_vault_crypto"
        private const val VERSION = "schema_version"
        private const val SALT = "key_salt"
        private const val WRAPPED_KEY = "wrapped_vault_key"
        private const val SCHEMA_VERSION = 1
        private const val MIN_PASSWORD_LENGTH = 6
        private const val PBKDF2_ITERATIONS = 310_000
        private const val KEY_BITS = 256
        private const val SALT_BYTES = 16
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val RECORD_PREFIX = "NESTNOTE_PRIVATE_RECORD_V1:"
        private const val RECORD_AAD = "com.nestnote.app|private-vault|record|v1"
        private fun recordAad(id: Long) = "$RECORD_AAD|$id".toByteArray(Charsets.UTF_8)
        private val WRAP_AAD = "com.nestnote.app|private-vault|wrapped-key|v1".toByteArray(Charsets.UTF_8)
        private val LOCK = Any()
    }
}
