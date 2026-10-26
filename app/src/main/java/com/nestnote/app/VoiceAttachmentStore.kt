package com.nestnote.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
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
 *
 * The note ID and attachment ID are bound into the GCM additional authenticated data. A file
 * therefore cannot be moved to another note's directory and still decrypt, which is why
 * [copyAll] re-encrypts rather than copying bytes.
 *
 * All instances share one lock. Callers create a new store per use, so a per-instance lock
 * would not stop a save from racing a delete.
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
        synchronized(LOCK) { removeStaleTempFiles() }
    }

    fun save(noteId: Long, attachmentId: String = UUID.randomUUID().toString(), input: InputStream): VoiceAttachment =
        synchronized(LOCK) { saveLocked(noteId, attachmentId, input) }

    fun read(noteId: Long, attachmentId: String): ByteArray = synchronized(LOCK) { readLocked(noteId, attachmentId) }

    fun delete(noteId: Long, attachmentId: String): Boolean = synchronized(LOCK) {
        validateIdentity(noteId, attachmentId)
        deleteLocked(noteId, attachmentId)
    }

    /**
     * Copies every attachment of [fromNoteId] to [toNoteId], re-encrypting each one under the
     * new note's identity. Used when a note is duplicated. All-or-nothing: if any copy fails,
     * the copies already written are removed and the exception is rethrown.
     * Returns the attachment IDs copied.
     */
    fun copyAll(fromNoteId: Long, toNoteId: Long): List<String> = synchronized(LOCK) {
        require(fromNoteId > 0 && toNoteId > 0 && fromNoteId != toNoteId) { "Invalid note IDs for copy" }
        val copied = mutableListOf<String>()
        try {
            for (attachment in listLocked(fromNoteId)) {
                val plaintext = readLocked(fromNoteId, attachment.attachmentId)
                saveLocked(toNoteId, attachment.attachmentId, ByteArrayInputStream(plaintext))
                copied += attachment.attachmentId
            }
        } catch (error: Exception) {
            copied.forEach { deleteLocked(toNoteId, it) }
            throw error
        }
        copied
    }

    /** Removes all attachments for a note after its deletion is committed. */
    fun deleteAll(noteId: Long): Boolean = synchronized(LOCK) {
        require(noteId > 0) { "Invalid note ID" }
        val dir = noteDirectory(noteId)
        if (!dir.exists()) return@synchronized true
        if (!dir.isDirectory) return@synchronized false
        val children = dir.listFiles() ?: return@synchronized false
        var success = true
        children.forEach { file -> if (!file.delete()) success = false }
        if (success && !dir.delete()) success = false
        success
    }

    /**
     * Removes numeric note directories that are not in [liveNoteIds].
     * Callers must not invoke this while note storage is in recovery-required state: an
     * incomplete live set would delete audio that still belongs to a note.
     */
    fun removeOrphans(liveNoteIds: Set<Long>): Int = synchronized(LOCK) {
        require(liveNoteIds.all { it > 0 }) { "Invalid live note IDs" }
        val directories = root.listFiles() ?: return@synchronized 0
        var removed = 0
        directories.filter { it.isDirectory && it.name.toLongOrNull() != null }
            .filter { it.name.toLong() !in liveNoteIds }
            .forEach { dir ->
                val files = dir.listFiles()
                if (files != null && files.all { it.isFile && it.delete() } && dir.delete()) removed++
            }
        removed
    }

    fun list(noteId: Long): List<VoiceAttachment> = synchronized(LOCK) { listLocked(noteId) }

    private fun saveLocked(noteId: Long, attachmentId: String, input: InputStream): VoiceAttachment {
        validateIdentity(noteId, attachmentId)
        val dir = noteDirectory(noteId)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Unable to create note attachment directory")
        val destination = File(dir, "$attachmentId$EXTENSION")
        if (destination.exists()) throw IOException("Voice attachment already exists")
        val temp = File(root, "$TEMP_PREFIX${noteId}_$attachmentId.${UUID.randomUUID()}$TEMP_SUFFIX")
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

    private fun readLocked(noteId: Long, attachmentId: String): ByteArray {
        validateIdentity(noteId, attachmentId)
        val file = File(noteDirectory(noteId), "$attachmentId$EXTENSION")
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

    private fun deleteLocked(noteId: Long, attachmentId: String): Boolean {
        val file = File(noteDirectory(noteId), "$attachmentId$EXTENSION")
        return !file.exists() || file.delete()
    }

    private fun listLocked(noteId: Long): List<VoiceAttachment> {
        require(noteId > 0) { "Invalid note ID" }
        val dir = noteDirectory(noteId)
        return (dir.listFiles() ?: emptyArray()).filter { it.isFile && it.name.endsWith(EXTENSION) }
            .mapNotNull { file ->
                val id = file.name.removeSuffix(EXTENSION)
                if (!ID_PATTERN.matches(id)) null else VoiceAttachment(noteId, id, file.length())
            }.sortedBy { it.attachmentId }
    }

    /** Leftover temp files from an interrupted save. Runs under the lock, so no save is in flight. */
    private fun removeStaleTempFiles() {
        (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.startsWith(TEMP_PREFIX) && it.name.endsWith(TEMP_SUFFIX) }
            .forEach { it.delete() }
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
        const val MAX_AUDIO_BYTES = 100 * 1024 * 1024
        private const val MAX_ENCRYPTED_BYTES = MAX_AUDIO_BYTES + HEADER_BYTES + TAG_BYTES
        private const val EXTENSION = ".nva"
        private const val TEMP_PREFIX = "."
        private const val TEMP_SUFFIX = ".tmp"
        private val MAGIC = byteArrayOf(0x4e, 0x56, 0x41, 0x01)
        private val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,80}")
        private val LOCK = Any()
    }
}
