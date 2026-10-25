package com.nestnote.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import org.json.JSONObject

/**
 * Device-bound encryption for ordinary notes. This key alias and AAD namespace are
 * deliberately independent from PrivateNoteCrypto and its password-wrapped vault key.
 */
internal class OrdinaryNoteCrypto(context: Context) {
    private val appContext = context.applicationContext

    fun encrypt(note: NestNote): String {
        require(!note.privateNote) { "Private Vault notes use their independent encryption boundary." }
        require(!note.encrypted) { "Note is already encrypted." }
        val payload = JSONObject().apply {
            put("id", note.id)
            put("title", note.title)
            put("body", note.body)
            put("category", note.category)
            put("color", note.color)
            put("pinned", note.pinned)
            put("reminder", note.reminder)
            put("updated", note.updated)
            put("created", note.created)
            put("archived", note.archived)
            put("background", note.background)
            put("wallpaperUri", note.wallpaperUri)
            put("format", note.format)
        }.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(aad(note.id))
        val encrypted = cipher.doFinal(payload)
        return PREFIX + Base64.encodeToString(
            ByteBuffer.allocate(cipher.iv.size + encrypted.size).put(cipher.iv).put(encrypted).array(),
            Base64.NO_WRAP
        )
    }

    fun decrypt(note: NestNote): NestNote {
        require(!note.privateNote) { "Private Vault notes cannot use ordinary-note encryption." }
        require(note.encrypted && note.encryptedPayload.startsWith(PREFIX)) { "Note is not encrypted with this format." }
        val raw = Base64.decode(note.encryptedPayload.removePrefix(PREFIX), Base64.NO_WRAP)
        require(raw.size >= IV_BYTES + TAG_BYTES) { "Encrypted note is incomplete." }
        val buffer = ByteBuffer.wrap(raw)
        val iv = ByteArray(IV_BYTES).also(buffer::get)
        val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(note.id))
        val payload = JSONObject(String(cipher.doFinal(ciphertext), Charsets.UTF_8))
        require(payload.getLong("id") == note.id) { "Encrypted note identity did not verify." }
        return note.copy(
            title = payload.getString("title"),
            body = payload.getString("body"),
            category = payload.optString("category", "Ideas"),
            color = payload.optInt("color"),
            pinned = payload.optBoolean("pinned"),
            reminder = payload.optString("reminder"),
            updated = payload.optLong("updated"),
            created = payload.optLong("created", note.id),
            archived = payload.optBoolean("archived"),
            background = payload.optInt("background").coerceIn(0, 9),
            wallpaperUri = payload.optString("wallpaperUri"),
            format = if (payload.optString("format") == "markdown") "markdown" else "plain",
            encrypted = false,
            encryptedPayload = ""
        )
    }

    private fun getExistingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)
            ?: throw IllegalStateException("The device encryption key is unavailable. The encrypted note was preserved.")
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "nestnote_ordinary_notes_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val PREFIX = "NESTNOTE_ORDINARY_NOTE_V1:"
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val TAG_BITS = 128
        private fun aad(id: Long) = "com.nestnote.app|ordinary-note|v1|$id".toByteArray(Charsets.UTF_8)
    }
}
