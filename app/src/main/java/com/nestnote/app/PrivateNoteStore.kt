package com.nestnote.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Separate persistence namespace for cryptographically isolated private records.
 * All methods require a successfully unlocked PrivateNoteCrypto.Session; this store never falls
 * back to NoteStore's ordinary encryption. Migration/Compose integration must be completed
 * before this store is used for production records.
 */
internal class PrivateNoteStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(session: PrivateNoteCrypto.Session): List<NestNote> {
        val version = prefs.getInt(VERSION, -1)
        val records = prefs.getString(RECORDS, null)
        if (version == -1 && records == null) return emptyList()
        require(version == SCHEMA_VERSION && !records.isNullOrBlank()) {
            "Private note store metadata is missing or unsupported"
        }
        val array = JSONArray(records)
        val result = ArrayList<NestNote>(array.length())
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.getLong("id")
            val envelope = item.getString("ciphertext")
            val plaintext = session.decrypt(envelope, id).toString(Charsets.UTF_8)
            val decoded = NoteStore.decode(plaintext) ?: throw IllegalArgumentException("Private note payload is malformed")
            require(decoded.size == 1)
            val note = decoded.single()
            require(note.id == id && note.privateNote) { "Private record classification mismatch" }
            result += note
        }
        require(result.size <= NoteStore.MAX_NOTES)
        require(result.map { it.id }.distinct().size == result.size) { "Duplicate private record IDs" }
        require(NoteStore.isValidNoteCollection(result)) { "Private note collection is invalid" }
        return result
    }

    /** Atomic preference commit; refuses public records rather than silently reclassifying them. */
    fun save(session: PrivateNoteCrypto.Session, notes: List<NestNote>): Boolean = runCatching {
        require(NoteStore.isValidNoteCollection(notes))
        require(notes.all { it.privateNote && it.id > 0L })
        require(notes.map { it.id }.distinct().size == notes.size)
        val array = JSONArray()
        notes.forEach { note ->
            val envelope = session.encrypt(NoteStore.encode(listOf(note)).toByteArray(Charsets.UTF_8), note.id)
            array.put(JSONObject().put("id", note.id).put("ciphertext", envelope))
        }
        prefs.edit().putInt(VERSION, SCHEMA_VERSION).putString(RECORDS, array.toString()).commit()
    }.getOrDefault(false)

    /** True if any store metadata exists, including malformed/unsupported data that must not be overwritten. */
    fun hasStoredRecords(): Boolean = prefs.contains(VERSION) || prefs.contains(RECORDS)

    fun clear(): Boolean = prefs.edit().remove(RECORDS).remove(VERSION).commit()

    companion object {
        private const val PREFS = "nestnote_private_note_records"
        private const val VERSION = "schema_version"
        private const val RECORDS = "encrypted_records"
        private const val SCHEMA_VERSION = 1
    }
}
