package com.nestnote.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

/** Durable, versioned metadata for encrypted voice attachments. Audio bytes remain in VoiceAttachmentStore. */
data class VoiceAttachmentMetadata(
    val noteId: Long,
    val attachmentId: String,
    val createdAtMillis: Long,
    val durationMillis: Long,
    val encryptedSizeBytes: Long,
    val displayName: String
)

/**
 * Atomic metadata index. The index is advisory: every entry is checked against encrypted storage
 * before it is returned, and recovery never deletes ciphertext merely because metadata is missing.
 */
class VoiceAttachmentManifest internal constructor(
    context: Context,
    rootOverride: File? = null
) {
    private val file = File(rootOverride ?: File(context.filesDir, "voice_attachments_v1"), MANIFEST_NAME)
    private val temp = File(file.parentFile, "$MANIFEST_NAME.tmp")

    @Synchronized
    fun list(noteId: Long, store: VoiceAttachmentStore): List<VoiceAttachmentMetadata> {
        require(noteId > 0)
        val saved = readAll()
        val actual = store.list(noteId).associateBy { key(it.noteId, it.attachmentId) }
        // Reconcile index entries with files; missing files are omitted, not recreated.
        val valid = saved.filter { (_, item) -> item.noteId != noteId || actual.containsKey(key(item.noteId, item.attachmentId)) }
        if (valid.size != saved.size) writeAll(valid)
        return valid.values.filter { it.noteId == noteId }.sortedBy { it.createdAtMillis }
    }

    @Synchronized
    fun put(metadata: VoiceAttachmentMetadata, store: VoiceAttachmentStore) {
        validate(metadata)
        val actual = store.list(metadata.noteId).firstOrNull { it.attachmentId == metadata.attachmentId }
            ?: throw IOException("Attachment payload is missing; metadata was not committed")
        if (actual.encryptedSizeBytes != metadata.encryptedSizeBytes) {
            throw IOException("Attachment size does not match encrypted payload")
        }
        val items = readAll().toMutableMap()
        items[key(metadata.noteId, metadata.attachmentId)] = metadata
        writeAll(items)
    }

    /** Remove index entries for a note draft only after its owning note is discarded. */
    @Synchronized
    fun removeNote(noteId: Long) {
        require(noteId > 0)
        val items = readAll().toMutableMap()
        val before = items.size
        items.entries.removeAll { (_, metadata) -> metadata.noteId == noteId }
        if (items.size != before) writeAll(items)
    }

    @Synchronized
    fun remove(noteId: Long, attachmentId: String) {
        require(noteId > 0 && attachmentId.matches(ID_PATTERN))
        val items = readAll().toMutableMap()
        val itemKey = key(noteId, attachmentId)
        val current = items[itemKey]
        if (current?.noteId == noteId) {
            items.remove(itemKey)
            writeAll(items)
        }
    }

    /** Keep files without metadata for recovery; only prune stale index entries. */
    @Synchronized
    fun reconcile(store: VoiceAttachmentStore, liveNoteIds: Set<Long>): Int {
        require(liveNoteIds.all { it > 0 })
        val items = readAll().toMutableMap()
        val before = items.size
        val actual = liveNoteIds.flatMap { id -> store.list(id).map { id to it } }
            .associate { (id, attachment) -> key(id, attachment.attachmentId) to (id to attachment) }
        items.entries.removeAll { (_, metadata) ->
            val actualAttachment = actual[key(metadata.noteId, metadata.attachmentId)]
            metadata.noteId !in liveNoteIds || actualAttachment?.first != metadata.noteId ||
                actualAttachment?.second?.encryptedSizeBytes != metadata.encryptedSizeBytes
        }
        if (items.size != before) writeAll(items)
        return before - items.size
    }

    private fun readAll(): Map<String, VoiceAttachmentMetadata> {
        if (!file.exists()) return emptyMap()
        return try {
            if (file.length() > MAX_MANIFEST_BYTES) {
                throw IOException("Attachment manifest exceeds size limit")
            }
            val root = JSONObject(file.readText(Charsets.UTF_8))
            if (root.optInt("schemaVersion", -1) != SCHEMA_VERSION) throw IOException("Unsupported attachment manifest version")
            val array = root.getJSONArray("attachments")
            require(array.length() <= MAX_ATTACHMENTS) { "Too many attachment records" }
            buildMap {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val item = VoiceAttachmentMetadata(
                        noteId = o.getLong("noteId"),
                        attachmentId = o.getString("attachmentId"),
                        createdAtMillis = o.getLong("createdAtMillis"),
                        durationMillis = o.getLong("durationMillis"),
                        encryptedSizeBytes = o.getLong("encryptedSizeBytes"),
                        displayName = o.getString("displayName")
                    )
                    validate(item)
                    require(put(key(item.noteId, item.attachmentId), item) == null) { "Duplicate attachment identity" }
                }
            }
        } catch (error: Exception) {
            throw IOException("Attachment metadata is unreadable; encrypted recordings were preserved", error)
        }
    }

    private fun writeAll(items: Map<String, VoiceAttachmentMetadata>) {
        if (items.size > MAX_ATTACHMENTS) throw IOException("Too many attachment records")
        file.parentFile?.let { if (!it.exists() && !it.mkdirs()) throw IOException("Cannot create attachment metadata directory") }
        val array = JSONArray()
        items.values.sortedWith(compareBy<VoiceAttachmentMetadata> { it.noteId }.thenBy { it.createdAtMillis }).forEach { item ->
            validate(item)
            array.put(JSONObject().apply {
                put("noteId", item.noteId); put("attachmentId", item.attachmentId)
                put("createdAtMillis", item.createdAtMillis); put("durationMillis", item.durationMillis)
                put("encryptedSizeBytes", item.encryptedSizeBytes); put("displayName", item.displayName)
            })
        }
        val payload = JSONObject().put("schemaVersion", SCHEMA_VERSION).put("attachments", array).toString()
        try {
            java.io.FileOutputStream(temp).use { stream ->
                stream.write(payload.toByteArray(Charsets.UTF_8)); stream.flush(); stream.fd.sync()
            }
            // Rename the fully synced temporary file over the old manifest without
            // deleting the old copy first. If replacement fails, the previous manifest survives.
            if (!temp.renameTo(file)) throw IOException("Cannot atomically replace attachment metadata")
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private fun key(noteId: Long, attachmentId: String): String = "$noteId:$attachmentId"

    private fun validate(item: VoiceAttachmentMetadata) {
        require(item.noteId > 0 && item.attachmentId.matches(ID_PATTERN))
        require(item.createdAtMillis >= 0 && item.durationMillis >= 0)
        require(item.encryptedSizeBytes > 0 && item.displayName.isNotBlank() && item.displayName.length <= 160)
    }

    companion object {
        private const val MANIFEST_NAME = "manifest_v1.json"
        private const val SCHEMA_VERSION = 1
        private const val MAX_ATTACHMENTS = 10000
        // Prevent unbounded allocation before JSON parsing; leaves room for 10,000 bounded records.
        private const val MAX_MANIFEST_BYTES = 4 * 1024 * 1024
        private val ID_PATTERN = Regex("[A-Za-z0-9_-]{1,80}")
    }
}
