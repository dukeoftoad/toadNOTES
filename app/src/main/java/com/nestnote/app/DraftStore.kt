package com.nestnote.app

import android.content.Context
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
class DraftStore internal constructor(rootOverride: File) {

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
        val format: String = "plain"
    )

    constructor(context: Context) : this(File(context.filesDir, DIRECTORY_NAME))

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

    fun listAll(): List<Draft> = synchronized(LOCK) {
        if (!root.isDirectory) return@synchronized emptyList<Draft>()
        (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".json") }
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
        val bytes = encode(draft).toByteArray(Charsets.UTF_8)
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
        if (file.length() > MAX_BYTES) null else decode(file.readText(Charsets.UTF_8))
    }.getOrNull()

    private fun fileFor(noteId: Long): File = File(root, "note-$noteId.json")

    private fun encode(draft: Draft): String {
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
        val writer = StringWriter()
        props.store(writer, null)
        return writer.toString()
    }

    private fun decode(raw: String): Draft? = runCatching { decodeOrNull(raw) }.getOrNull()

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
            format = if (props.getProperty("format") == "markdown") "markdown" else "plain"
        )
    }

    companion object {
        private const val DIRECTORY_NAME = "drafts"
        private const val SCHEMA_VERSION = 1
        private const val TMP_SUFFIX = ".tmp"
        private const val MAX_BYTES = 8 * 1024 * 1024
        private val LOCK = Any()
    }
}
