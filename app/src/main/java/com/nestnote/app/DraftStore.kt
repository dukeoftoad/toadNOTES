package com.nestnote.app

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

/**
 * Persists in-progress note drafts so an interrupted edit survives process death.
 *
 * Each draft is one file (`note-<id>.json`) in `filesDir/drafts/`. A write goes to a
 * `.tmp` file, is fsynced, and is then renamed over the target, so the previous draft
 * stays on disk until the new one is complete. Reads never throw: a corrupted or
 * missing file yields null.
 *
 * All instances share one lock. The editor creates a new DraftStore per call, so a
 * per-instance lock would not stop a write and a clear from interleaving.
 *
 * Storage format: Java [Properties] text (UTF-8). Schema version 1. Fields added after
 * version 1 (such as `format`) are optional on read, so older drafts still decode.
 *
 * The class never deletes notes; it only manages draft snapshots.
 */
class DraftStore internal constructor(rootOverride: File, private val privateKeyProvider: (() -> SecretKey)? = null) {

    data class Draft(
        val noteId: Long,
        val title: String,
        val body: String,
        val category: String,
        val color: Int,
        val background: Int,
        val pinned: Boolean,
        val reminder: String,
        val wallpaperUri: String,
        val savedAtMillis: Long,
        /** "plain" or "markdown". Defaults to plain for drafts written before this field existed. */
        val format: String = "plain",
        val privateNote: Boolean = false
    )

    constructor(context: Context) : this(File(context.filesDir, DIRECTORY_NAME), { getOrCreatePrivateDraftKey() })

    private val root = rootOverride

    init {
        if (root.exists() && !root.isDirectory) throw IOException("Draft root is not a directory")
    }

    fun write(draft: Draft): Boolean = synchronized(LOCK) { writeLocked(draft) }

    fun read(noteId: Long): Draft? = synchronized(LOCK) { readLocked(noteId) }

    fun clear(noteId: Long): Boolean = synchronized(LOCK) {
        if (noteId <= 0L) return@synchronized false
        val file = fileFor(noteId)
        !file.exists() || file.delete()
    }

    fun listAll(includePrivate: Boolean = false): List<Draft> = synchronized(LOCK) {
        if (!root.isDirectory) return@synchronized emptyList<Draft>()
        (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".json") }
            .filter { file ->
                if (includePrivate) true else {
                    val prefix = runCatching {
                        file.bufferedReader(Charsets.UTF_8).use { it.readLine().orEmpty() }
                    }.getOrDefault("")
                    when {
                        prefix.startsWith(PRIVATE_PREFIX) -> false
                        prefix.startsWith(PUBLIC_PREFIX) -> true
                        else -> { file.delete(); false } // legacy plaintext: fail closed and discard
                    }
                }
            }
            .mapNotNull { readFile(it) }
    }

    fun clearAll(): Int = synchronized(LOCK) {
        if (!root.isDirectory) return@synchronized 0
        var removed = 0
        (root.listFiles() ?: emptyArray()).forEach { if (it.isFile && it.delete()) removed++ }
        removed
    }

    private fun writeLocked(draft: Draft): Boolean {
        if (draft.noteId <= 0L) return false
        val bytes = runCatching { encodeEnvelope(draft).toByteArray(Charsets.UTF_8) }.getOrNull() ?: return false
        // Refuse oversized drafts here instead of writing something read() would later reject.
        if (bytes.size > MAX_BYTES) return false
        return runCatching {
            if (!root.isDirectory && !root.mkdirs()) return false
            val target = fileFor(draft.noteId)
            val temp = File(root, target.name + TMP_SUFFIX)
            FileOutputStream(temp).use { out ->
                out.write(bytes)
                out.flush()
                out.fd.sync()
            }
            // renameTo replaces the target atomically on Linux/Android. Only fall back to
            // delete-then-rename where the platform refuses to overwrite.
            if (temp.renameTo(target)) return true
            if (target.delete() && temp.renameTo(target)) return true
            temp.delete()
            false
        }.getOrDefault(false)
    }

    private fun readLocked(noteId: Long): Draft? {
        if (noteId <= 0L) return null
        val file = fileFor(noteId)
        if (!file.isFile) return null
        return readFile(file)
    }

    private fun readFile(file: File): Draft? = runCatching {
        if (file.length() > MAX_BYTES) return@runCatching null
        val raw = file.readText(Charsets.UTF_8)
        if (!raw.startsWith(PUBLIC_PREFIX) && !raw.startsWith(PRIVATE_PREFIX)) {
            // Old schema-1 files were plaintext. Do not parse/reclassify them; remove the
            // legacy file on discovery so recovery cannot surface it as an ordinary draft.
            file.delete()
            null
        } else {
            decodeEnvelope(raw)
        }
    }.getOrNull()

    private fun fileFor(noteId: Long): File = File(root, "note-$noteId.json")

    private fun encodeProperties(draft: Draft): String {
        val props = Properties()
        props["schemaVersion"] = SCHEMA_VERSION.toString()
        props["noteId"] = draft.noteId.toString()
        props["title"] = draft.title
        props["body"] = draft.body
        props["category"] = draft.category
        props["color"] = draft.color.toString()
        props["background"] = draft.background.toString()
        props["pinned"] = draft.pinned.toString()
        props["reminder"] = draft.reminder
        props["wallpaperUri"] = draft.wallpaperUri
        props["savedAtMillis"] = draft.savedAtMillis.toString()
        props["format"] = if (draft.format == "markdown") "markdown" else "plain"
        props["privateNote"] = draft.privateNote.toString()
        val writer = StringWriter()
        props.store(writer, null)
        return writer.toString()
    }


    // Versioned envelopes ensure old plaintext files are rejected, not silently migrated.
    private fun encodeEnvelope(draft: Draft): String {
        val plaintext = encodeProperties(draft).toByteArray(Charsets.UTF_8)
        if (!draft.privateNote) return PUBLIC_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(plaintext)
        val key = privateKeyProvider?.invoke() ?: throw IllegalStateException("Private draft key unavailable")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(PRIVATE_AAD)
        val ciphertext = cipher.doFinal(plaintext)
        val payload = ByteBuffer.allocate(cipher.iv.size + ciphertext.size).put(cipher.iv).put(ciphertext).array()
        return PRIVATE_PREFIX + Base64.getEncoder().withoutPadding().encodeToString(payload)
    }

    private fun decodeEnvelope(raw: String): Draft? {
        val isPrivate = raw.startsWith(PRIVATE_PREFIX)
        val plain = when {
            raw.startsWith(PUBLIC_PREFIX) -> {
                val bytes = Base64.getDecoder().decode(raw.removePrefix(PUBLIC_PREFIX).trim())
                Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
            }
            isPrivate -> {
                val key = privateKeyProvider?.invoke() ?: return null
                val payload = Base64.getDecoder().decode(raw.removePrefix(PRIVATE_PREFIX).trim())
                if (payload.size < IV_BYTES + TAG_BYTES) return null
                val buffer = ByteBuffer.wrap(payload)
                val iv = ByteArray(IV_BYTES); buffer.get(iv)
                val encrypted = ByteArray(buffer.remaining()); buffer.get(encrypted)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
                cipher.updateAAD(PRIVATE_AAD)
                val bytes = cipher.doFinal(encrypted)
                Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
            }
            else -> return null // Legacy plaintext / unknown format: fail closed.
        }
        val draft = decodeProperties(plain) ?: return null
        return draft.takeIf { it.privateNote == isPrivate }
    }

    private fun decodeProperties(raw: String): Draft? = runCatching { decodeOrNull(raw) }.getOrNull()

    private fun decodeOrNull(raw: String): Draft? {
        val props = Properties()
        props.load(StringReader(raw))
        val schemaVersion = props.getProperty("schemaVersion")?.toIntOrNull() ?: return null
        if (schemaVersion != SCHEMA_VERSION) return null
        val noteId = props.getProperty("noteId")?.toLongOrNull() ?: return null
        if (noteId <= 0L) return null
        return Draft(
            noteId = noteId,
            title = props.getProperty("title") ?: "",
            body = props.getProperty("body") ?: "",
            category = props.getProperty("category") ?: "Ideas",
            color = props.getProperty("color")?.toIntOrNull() ?: 0,
            background = props.getProperty("background")?.toIntOrNull() ?: 0,
            pinned = props.getProperty("pinned")?.toBooleanStrictOrNull() ?: false,
            reminder = props.getProperty("reminder") ?: "",
            wallpaperUri = props.getProperty("wallpaperUri") ?: "",
            savedAtMillis = props.getProperty("savedAtMillis")?.toLongOrNull() ?: 0L,
            format = if (props.getProperty("format") == "markdown") "markdown" else "plain",
            privateNote = props.getProperty("privateNote")?.toBooleanStrictOrNull() ?: return null
        )
    }

    companion object {
        private const val DIRECTORY_NAME = "drafts"
        private const val SCHEMA_VERSION = 2
        private const val TMP_SUFFIX = ".tmp"
        private const val MAX_BYTES = 8 * 1024 * 1024
        private const val KEY_ALIAS = "nestnote_private_drafts_aes_gcm_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_BYTES = 12
        private const val TAG_BYTES = 16
        private const val TAG_BITS = 128
        private const val PUBLIC_PREFIX = "NESTNOTE_DRAFT_PUBLIC_V2:"
        private const val PRIVATE_PREFIX = "NESTNOTE_DRAFT_PRIVATE_V2:"
        private val PRIVATE_AAD = "com.nestnote.app|drafts|private|v2".toByteArray(Charsets.UTF_8)
        private val LOCK = Any()

        private fun getOrCreatePrivateDraftKey(): SecretKey {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            (ks.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
            val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            gen.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).setKeySize(256).build())
            return gen.generateKey()
        }
    }
}
