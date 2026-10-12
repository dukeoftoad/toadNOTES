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
import org.json.JSONArray
import org.json.JSONObject

data class NestNote(
    val id: Long = System.currentTimeMillis(),
    val title: String,
    val body: String,
    val category: String = "Ideas",
    val color: Int = 0,
    val pinned: Boolean = false,
    val reminder: String = "",
    val updated: Long = System.currentTimeMillis(),
    val created: Long = System.currentTimeMillis(),
    val archived: Boolean = false,
    val background: Int = 0
)

data class MergeResult(val notes: List<NestNote>, val added: Int, val updated: Int)
data class NoteLoadResult(val notes: List<NestNote>, val recoveryRequired: Boolean)
class NoteStorageException(message: String, cause: Throwable? = null) : IllegalStateException(message, cause)

class NoteStore internal constructor(context: Context, private val testKeyProvider: (() -> SecretKey)? = null) {
    private val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)

    fun load(): List<NestNote> = loadSafely().notes

    /**
     * Distinguishes a genuinely new install from unreadable stored data.
     * On corruption, return no editable notes and leave the original bytes untouched.
     */
    fun loadSafely(): NoteLoadResult {
        val encrypted = prefs.getString(ENCRYPTED_NOTES_KEY, null)
        if (encrypted != null) {
            val decoded = runCatching { decode(decrypt(encrypted)) }.getOrNull()
            return if (decoded != null) NoteLoadResult(decoded, false)
            else NoteLoadResult(emptyList(), true)
        }

        val legacy = prefs.getString(LEGACY_NOTES_KEY, null)
            ?: return NoteLoadResult(sampleNotes(), false)
        val decoded = decode(legacy)
        if (decoded == null) return NoteLoadResult(emptyList(), true)

        // Commit the encrypted payload before removing the plaintext key. If encryption or
        // the preferences commit fails, preserve the legacy bytes and block normal writes.
        return if (save(decoded)) NoteLoadResult(decoded, false)
        else NoteLoadResult(emptyList(), true)
    }

    /** Encrypts first and removes legacy plaintext only in the same successful preferences commit. */
    fun save(notes: List<NestNote>): Boolean {
        if (!isValidNoteCollection(notes)) return false
        return runCatching {
            val encrypted = encrypt(encode(notes))
            prefs.edit()
                .putString(ENCRYPTED_NOTES_KEY, encrypted)
                .remove(LEGACY_NOTES_KEY)
                .commit()
        }.getOrDefault(false)
    }

    private fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(cipher.iv.size + ciphertext.size)
            .put(cipher.iv).put(ciphertext).array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        require(payload.size > GCM_IV_LENGTH_BYTES) { "Encrypted note payload is too short" }
        val buffer = ByteBuffer.wrap(payload)
        val iv = ByteArray(GCM_IV_LENGTH_BYTES)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getExistingKey(): SecretKey {
        testKeyProvider?.let { return it() }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
            ?: throw IllegalStateException("The encryption key for saved notes is missing")
    }

    private fun getOrCreateKey(): SecretKey {
        testKeyProvider?.let { return it() }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        private const val LEGACY_NOTES_KEY = "notes"
        private const val ENCRYPTED_NOTES_KEY = "notes_encrypted_v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "nestnote_notes_aes_gcm_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_IV_LENGTH_BYTES = 12
        private const val GCM_TAG_LENGTH_BITS = 128

        const val MAX_BACKUP_BYTES = 10 * 1024 * 1024
        const val MAX_NOTES = 10_000
        const val MAX_TITLE_LENGTH = 10_000
        const val MAX_BODY_LENGTH = 1_000_000
        const val MAX_CATEGORY_LENGTH = 256
        const val MAX_REMINDER_LENGTH = 512
        const val CURRENT_BACKUP_SCHEMA_VERSION = 1

        /** Serializes notes to the same JSON array used for local storage and backups. */
        fun encode(notes: List<NestNote>): String {
            val array = JSONArray()
            notes.forEach { n ->
                array.put(JSONObject().apply {
                    put("id", n.id); put("title", n.title); put("body", n.body)
                    put("category", n.category); put("color", n.color)
                    put("pinned", n.pinned); put("reminder", n.reminder)
                    put("updated", n.updated); put("created", n.created); put("archived", n.archived)
                    put("background", n.background.coerceIn(0, 5))
                })
            }
            return array.toString()
        }

        /** Encodes a versioned user-facing backup envelope. Local storage keeps the legacy array format. */
        fun encodeBackup(notes: List<NestNote>): String {
            require(isValidNoteCollection(notes)) { "invalid note records" }
            return JSONObject()
                .put("schemaVersion", CURRENT_BACKUP_SCHEMA_VERSION)
                .put("notes", JSONArray(encode(notes)))
                .toString()
        }

        /** Accepts versioned backups and legacy JSON-array backups for backward compatibility. */
        fun decodeBackup(raw: String): List<NestNote>? = runCatching {
            require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) { "backup is too large" }
            if (raw.trimStart().startsWith("[")) {
                decode(raw, requireFields = true)
            } else {
                val envelope = JSONObject(raw)
                require(envelope.optInt("schemaVersion", -1) == CURRENT_BACKUP_SCHEMA_VERSION) {
                    "unsupported backup schema"
                }
                val records = envelope.optJSONArray("notes") ?: error("missing notes")
                decode(records.toString(), requireFields = true) ?: error("invalid backup records")
            }
        }.getOrNull()

        /**
         * Parses a JSON array of notes. Returns null if the text is not valid.
         * With [requireFields] every record must carry at least an id and a title
         * (used to reject unrelated files during import).
         */
        fun decode(raw: String, requireFields: Boolean = false): List<NestNote>? = runCatching {
            require(raw.toByteArray(Charsets.UTF_8).size <= MAX_BACKUP_BYTES) { "backup is too large" }
            val array = JSONArray(raw)
            require(array.length() <= MAX_NOTES) { "too many notes" }
            val decoded = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                if (requireFields && !(o.has("id") && o.has("title"))) error("not a NESTNOTE record")
                NestNote(
                    id = o.optLong("id"),
                    title = o.optString("title"),
                    body = o.optString("body"),
                    category = o.optString("category", "Ideas"),
                    color = o.optInt("color"),
                    pinned = o.optBoolean("pinned"),
                    reminder = o.optString("reminder"),
                    updated = o.optLong("updated"),
                    created = o.optLong("created", o.optLong("id", System.currentTimeMillis())),
                    archived = o.optBoolean("archived", false),
                    background = o.optInt("background", 0).coerceIn(0, 5)
                )
            }
            require(isValidNoteCollection(decoded)) { "invalid note records" }
            decoded
        }.getOrNull()

        /** Prevents a draft based on an older saved version from replacing newer content. */
        fun isSameNoteVersion(expectedUpdated: Long, currentUpdated: Long): Boolean =
            expectedUpdated == currentUpdated

        /** Shared validation for persisted and imported note collections. */
        fun isValidNoteCollection(notes: List<NestNote>): Boolean {
            if (notes.size > MAX_NOTES) return false
            if (notes.any { note ->
                    note.id <= 0L ||
                        note.title.length > MAX_TITLE_LENGTH ||
                        note.body.length > MAX_BODY_LENGTH ||
                        note.category.length > MAX_CATEGORY_LENGTH ||
                        note.reminder.length > MAX_REMINDER_LENGTH ||
                        note.updated < 0L || note.created < 0L
                }) return false
            return notes.map { it.id }.distinct().size == notes.size
        }

        /** Merges imported notes by id: new ids are added, existing ids keep the most recently edited copy. */
        fun merge(existing: List<NestNote>, incoming: List<NestNote>): MergeResult {
            val byId = existing.associateBy { it.id }.toMutableMap()
            var added = 0
            var updated = 0
            incoming.forEach { note ->
                val current = byId[note.id]
                when {
                    current == null -> { byId[note.id] = note; added++ }
                    note.updated > current.updated -> { byId[note.id] = note; updated++ }
                }
            }
            val order = existing.map { it.id } + incoming.map { it.id }.filter { id -> existing.none { it.id == id } }
            return MergeResult(order.distinct().mapNotNull { byId[it] }, added, updated)
        }
    }

    private fun sampleNotes() = listOf(
        NestNote(1, "Project Plan", "1. Finalize UI design\n2. Develop core features\n3. Test and fix bugs\n4. Prepare for launch", "Work", 0, true),
        NestNote(2, "Trip to the Mountains", "A little weekend escape to breathe fresh air, find a quiet trail, and watch the sunrise. Remember to pack the camera!", "Personal", 4),
        NestNote(3, "App Ideas", "A focus timer that feels calm, a daily reflection prompt, and a tiny widget for capturing ideas.", "Ideas", 2),
        NestNote(4, "Shopping List", "Oat milk\nFresh berries\nSourdough bread\nCoffee beans", "Personal", 5),
        NestNote(5, "Morning Routine", "Drink water, stretch for ten minutes, and journal three things I'm grateful for.", "Health", 3),
        NestNote(6, "Learning Kotlin", "Review Compose state, practice coroutines, and build something small.", "Education", 1)
    )
}
