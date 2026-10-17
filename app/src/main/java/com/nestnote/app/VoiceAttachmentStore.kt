package com.nestnote.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class VoiceAttachment(val noteId: Long, val attachmentId: String, val encryptedSizeBytes: Long)

/**
 * App-private voice attachment storage. Input is encrypted as it is streamed, so this layer never
 * stages plaintext audio on disk. Reads return bytes only after GCM authentication succeeds.
 *
 * Envelope: 4-byte magic/version, 12-byte random IV, ciphertext and 16-byte GCM tag.
 */
class VoiceAttachmentStore internal constructor(
    context: Context,
    private val keyProvider: (() -> SecretKey)? = null,
    rootOverride: File? = null
) {
    private val root = rootOverride ?: File(context.filesDir, DIRECTORY_NAME)

    init {
        if (!root.exists() && !root.mkdirs()) throw IOException("Unable to create private voice-note storage")
        if (!root.isDirectory) throw IOException("Voice-note storage is not a directory")
    }

    @Synchronized
    fun save(noteId: Long, attachmentId: String = UUID.randomUUID().toString(), input: InputStream): VoiceAttachment {
        validateIdentity(noteId, attachmentId)
        val destination = attachmentFile(noteId, attachmentId)
        if (destination.exists()) throw IOException("Voice attachment already exists")
        val temp = File(root, ".${noteId}_${attachmentId}.${UUID.randomUUID()}.tmp")
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            cipher.updateAAD(aad(noteId, attachmentId))
            require(cipher.iv.size == IV_BYTES) { "Unexpected AES-GCM IV size" }
            FileOutputStreamCompat(temp).use { output ->
                output.write(MAGIC)
                output.write(cipher.iv)
                val buffer = ByteArray(BUFFER_BYTES)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (count == 0) continue
                    total += count
                    if (total > MAX_AUDIO_BYTES) throw IOException("Voice attachment exceeds size limit")
                    val encrypted = cipher.update(buffer, 0, count)
                    if (encrypted != null && encrypted.isNotEmpty()) output.write(encrypted)
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes.isNotEmpty()) output.write(finalBytes)
                output.flushAndSync()
            }
            if (destination.exists()) throw IOException("Voice attachment already exists")
            if (!temp.renameTo(destination)) throw IOException("Unable to commit encrypted voice attachment")
            return VoiceAttachment(noteId, attachmentId, destination.length())
        } catch (error: Exception) {
            temp.delete()
            throw error
        }
    }

    @Synchronized
    fun read(noteId: Long, attachmentId: String): ByteArray {
        validateIdentity(noteId, attachmentId)
        val file = attachmentFile(noteId, attachmentId)
        if (!file.isFile) throw IOException("Voice attachment not found")
        if (file.length() < HEADER_BYTES + TAG_BYTES || file.length() > MAX_ENCRYPTED_BYTES) {
            throw IOException("Invalid voice attachment size")
        }
        val payload = FileInputStream(file).use { stream ->
            val output = ByteArrayOutputStream(file.length().toInt())
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                output.write(buffer, 0, count)
                if (output.size().toLong() > MAX_ENCRYPTED_BYTES) throw IOException("Voice attachment exceeds size limit")
            }
            output.toByteArray()
        }
        if (!payload.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) throw IOException("Unsupported voice attachment format")
        val iv = payload.copyOfRange(MAGIC.size, HEADER_BYTES)
        val ciphertext = payload.copyOfRange(HEADER_BYTES, payload.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(aad(noteId, attachmentId))
        return try {
            cipher.doFinal(ciphertext).also {
                if (it.size > MAX_AUDIO_BYTES) throw IOException("Voice attachment exceeds size limit")
            }
        } catch (error: Exception) {
            throw IOException("Voice attachment authentication failed", error)
        }
    }

    @Synchronized
    fun delete(noteId: Long, attachmentId: String): Boolean {
        validateIdentity(noteId, attachmentId)
        val file = attachmentFile(noteId, attachmentId)
        return !file.exists() || file.delete()
    }

    /** Removes all attachments for a note after its deletion is committed. */
    @Synchronized
    fun deleteAll(noteId: Long): Boolean {
        require(noteId > 0) { "Invalid note ID" }
        val dir = noteDirectory(noteId)
        if (!dir.exists()) return true
        if (!dir.isDirectory) return false
        val children = dir.listFiles() ?: return false
        var success = true
        children.forEach { file -> if (!file.delete()) success = false }
        if (success && !dir.delete()) success = false
        return success
    }

    /**
     * Removes numeric note directories that no longer belong to a trustworthy, committed note set.
     * Callers must not invoke this while note storage is in recovery-required state.
     */
    @Synchronized
    fun removeOrphans(liveNoteIds: Set<Long>): Int {
        require(liveNoteIds.all { it > 0 }) { "Invalid live note IDs" }
        val directories = root.listFiles() ?: return 0
        var removed = 0
        directories.filter { it.isDirectory && it.name.toLongOrNull() != null }
            .filter { it.name.toLong() !in liveNoteIds }
            .forEach { dir ->
                val files = dir.listFiles()
                if (files != null && files.all { it.isFile && it.delete() } && dir.delete()) removed++
            }
        return removed
    }

    @Synchronized
    fun list(noteId: Long): List<VoiceAttachment> {
        require(noteId > 0) { "Invalid note ID" }
        val dir = noteDirectory(noteId)
        return (dir.listFiles() ?: emptyArray()).filter { it.isFile && it.name.endsWith(EXTENSION) }
            .mapNotNull { file ->
                val id = file.name.removeSuffix(EXTENSION)
                if (!ID_PATTERN.matches(id)) null else VoiceAttachment(noteId, id, file.length())
            }.sortedBy { it.attachmentId }
    }

    private fun attachmentFile(noteId: Long, attachmentId: String): File {
        val dir = noteDirectory(noteId)
        if (!dir.exists() && !dir.mkdirs()) throw IOException("Unable to create note attachment directory")
        if (!dir.isDirectory) throw IOException("Invalid note attachment directory")
        return File(dir, "$attachmentId$EXTENSION")
    }

    private fun noteDirectory(noteId: Long) = File(root, noteId.toString())

    private fun validateIdentity(noteId: Long, attachmentId: String) {
        require(noteId > 0) { "Invalid note ID" }
        require(ID_PATTERN.matches(attachmentId)) { "Invalid voice attachment ID" }
    }

    private fun aad(noteId: Long, attachmentId: String) =
        "com.nestnote.app|voice|v1|$noteId|$attachmentId".toByteArray(Charsets.UTF_8)

    private fun getExistingKey(): SecretKey {
        keyProvider?.let { return it() }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: throw IOException("Voice-note encryption key is missing")
    }

    private fun getOrCreateKey(): SecretKey {
        keyProvider?.let { return it() }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private class FileOutputStreamCompat(file: File) : java.io.FileOutputStream(file) {
        fun flushAndSync() { flush(); fd.sync() }
    }

    companion object {
        private const val DIRECTORY_NAME = "voice_attachments_v1"
        private const val KEY_ALIAS = "nestnote_voice_aes_gcm_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val TAG_BITS = TAG_BYTES * 8
        private const val HEADER_BYTES = 4 + IV_BYTES
        private const val BUFFER_BYTES = 16 * 1024
        const val MAX_AUDIO_BYTES = 25 * 1024 * 1024
        private const val MAX_ENCRYPTED_BYTES = MAX_AUDIO_BYTES + HEADER_BYTES + TAG_BYTES
        private const val EXTENSION = ".nva"
        private val MAGIC = byteArrayOf(0x4e, 0x56, 0x41, 0x01)
        private val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,80}")
    }
}
