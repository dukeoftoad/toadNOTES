package com.nestnote.app

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Password-protected NESTNOTE archive.
 *
 * The payload is a ZIP (backup.json plus a small manifest) wrapped in AES-256-GCM.
 * A random salt and nonce are stored in the envelope; the password is never persisted.
 * The .nestnote extension intentionally identifies this encrypted container rather than
 * advertising it as a standard interoperable ZIP archive.
 */
internal object PasswordBackupArchive {
    private val MAGIC = byteArrayOf(0x4e, 0x45, 0x53, 0x54, 0x50, 0x42, 0x4b, 0x31) // NESTPBK1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val PBKDF2_ITERATIONS = 210_000
    private const val MAX_ARCHIVE_BYTES = NoteStore.MAX_ARCHIVE_PAYLOAD_BYTES
    private const val ARCHIVE_PAYLOAD_ENTRY = "archive-payload.json"
    private const val LEGACY_BACKUP_ENTRY = "backup.json"
    private const val MANIFEST_ENTRY = "manifest.txt"

    fun encryptArchive(archivePayload: String, password: CharArray): ByteArray {
        require(password.size >= 10) { "Use a password with at least 10 characters" }
        require(archivePayload.toByteArray(Charsets.UTF_8).size <= MAX_ARCHIVE_BYTES) { "Archive payload is too large" }
        require(NoteStore.decodeArchivePayload(archivePayload) != null) { "Invalid NESTNOTE archive payload" }

        val zipBytes = makeZip(archivePayload)
        require(zipBytes.size <= MAX_ARCHIVE_BYTES) { "Encrypted archive is too large" }
        val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
        val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
        val key = deriveKey(password, salt)
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(MAGIC + salt + nonce)
            val encrypted = cipher.doFinal(zipBytes)
            ByteBuffer.allocate(MAGIC.size + SALT_BYTES + NONCE_BYTES + encrypted.size)
                .put(MAGIC).put(salt).put(nonce).put(encrypted).array()
                .also { require(it.size <= MAX_ARCHIVE_BYTES) { "Encrypted archive is too large" } }
        } finally {
            key.encoded?.fill(0)
        }
    }

    /** Returns null for wrong passwords, tampered archives, malformed ZIPs or invalid archive payloads. */
    fun decryptArchive(archive: ByteArray, password: CharArray): String? {
        if (password.size < 10 || archive.size > MAX_ARCHIVE_BYTES) return null
        val headerSize = MAGIC.size + SALT_BYTES + NONCE_BYTES
        if (archive.size < headerSize + 16) return null
        val input = ByteBuffer.wrap(archive)
        val magic = ByteArray(MAGIC.size).also(input::get)
        if (!magic.contentEquals(MAGIC)) return null
        val salt = ByteArray(SALT_BYTES).also(input::get)
        val nonce = ByteArray(NONCE_BYTES).also(input::get)
        val ciphertext = ByteArray(input.remaining()).also(input::get)
        val key = runCatching { deriveKey(password, salt) }.getOrNull() ?: return null
        val zipBytes = try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, nonce))
            cipher.updateAAD(MAGIC + salt + nonce)
            cipher.doFinal(ciphertext)
        } catch (_: AEADBadTagException) {
            return null
        } catch (_: Exception) {
            return null
        } finally {
            key.encoded?.fill(0)
        }
        if (zipBytes.size > MAX_ARCHIVE_BYTES) return null
        val entries = runCatching { readZip(zipBytes) }.getOrNull() ?: return null
        val json = entries[ARCHIVE_PAYLOAD_ENTRY] ?: entries[LEGACY_BACKUP_ENTRY] ?: return null
        // Manifest is required so arbitrary ZIPs cannot masquerade as NESTNOTE exports.
        if (entries[MANIFEST_ENTRY] != "format=com.nestnote.password-backup\nschema=1") return null
        return json.takeIf { NoteStore.decodeArchivePayload(it) != null }
    }

    private fun makeZip(json: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write("format=com.nestnote.password-backup\nschema=1".toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ARCHIVE_PAYLOAD_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bytes.toByteArray()
    }

    private fun readZip(bytes: ByteArray): Map<String, String> {
        val entries = linkedMapOf<String, String>()
        var total = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                require(!entry.isDirectory && entry.name in setOf(ARCHIVE_PAYLOAD_ENTRY, LEGACY_BACKUP_ENTRY, MANIFEST_ENTRY)) {
                    "Unexpected archive entry"
                }
                require(!entries.containsKey(entry.name)) { "Duplicate archive entry" }
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                while (true) {
                    val count = zip.read(buffer)
                    if (count < 0) break
                    total += count
                    require(total <= MAX_ARCHIVE_BYTES) { "Archive expands beyond supported size" }
                    output.write(buffer, 0, count)
                }
                entries[entry.name] = output.toByteArray().toString(Charsets.UTF_8)
                zip.closeEntry()
            }
        }
        return entries
    }

    private fun deriveKey(password: CharArray, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            SecretKeySpec(bytes, "AES").also { bytes.fill(0) }
        } finally {
            spec.clearPassword()
        }
    }
}
