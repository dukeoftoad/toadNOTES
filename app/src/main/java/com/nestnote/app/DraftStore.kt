package com.nestnote.app

import android.content.Context
import java.io.File
import java.io.IOException
import java.io.StringReader
import java.io.StringWriter
import java.util.Properties

/**
 * Persists in-progress note drafts so an interrupted edit survives process death.
 *
 * Each draft is one file (`note-<id>.json`) in `filesDir/drafts/`. Writes are
 * atomic (write to `.tmp`, then rename) so a crash mid-write never corrupts the
 * previous draft. Reads never throw — a corrupted or missing file simply yields
 * null, matching the recovery-safe posture of the rest of the app.
 *
 * Storage format: Java [Properties] text format (ISO-8859-1 default, but we use
 * UTF-8 explicitly on both sides). This avoids depending on `org.json`, whose
 * Android stub throws on the JVM and would force a classpath trick in unit tests.
 * Each field is a plain key=value line; multi-line bodies are automatically
 * escaped by `Properties.store`. The file extension stays `.json` for backward
 * compatibility with any existing tests or scripts looking for it.
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
        val savedAtMillis: Long
    )

    constructor(context: Context) : this(File(context.filesDir, DIRECTORY_NAME))

    private val root = rootOverride

    init {
        if (root.exists() && !root.isDirectory) throw IOException("Draft root is not a directory")
    }

    @Synchronized
    fun write(draft: Draft): Boolean {
        if (draft.noteId <= 0L) return false
        return runCatching {
            if (!root.exists() && !root.mkdirs()) return false
            val target = fileFor(draft.noteId)
            val temp = File(root, target.name + TMP_SUFFIX)
            temp.writeText(encode(draft), Charsets.UTF_8)
            if (target.exists() && !target.delete()) return false
            if (!temp.renameTo(target)) return false
            true
        }.getOrDefault(false)
    }

    @Synchronized
    fun read(noteId: Long): Draft? {
        if (noteId <= 0L) return null
        val file = fileFor(noteId)
        if (!file.isFile) return null
        return runCatching {
            if (file.length() > MAX_BYTES) return null
            decode(file.readText(Charsets.UTF_8))
        }.getOrNull()
    }

    @Synchronized
    fun clear(noteId: Long): Boolean {
        if (noteId <= 0L) return false
        val file = fileFor(noteId)
        return !file.exists() || file.delete()
    }

    @Synchronized
    fun listAll(): List<Draft> {
        if (!root.isDirectory) return emptyList()
        return (root.listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".json") }
            .mapNotNull { file ->
                runCatching {
                    if (file.length() > MAX_BYTES) return@runCatching null
                    decode(file.readText(Charsets.UTF_8))
                }.getOrNull()
            }
    }

    @Synchronized
    fun clearAll(): Int {
        if (!root.isDirectory) return 0
        var removed = 0
        (root.listFiles() ?: emptyArray()).forEach { if (it.isFile && it.delete()) removed++ }
        return removed
    }

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
        val writer = StringWriter()
        props.store(writer, null)
        return writer.toString()
    }

    private fun decode(raw: String): Draft? = runCatching {
        val props = Properties()
        props.load(StringReader(raw))
        val schemaVersion = props.getProperty("schemaVersion")?.toIntOrNull() ?: return null
        if (schemaVersion != SCHEMA_VERSION) return null
        val noteId = props.getProperty("noteId")?.toLongOrNull() ?: return null
        if (noteId <= 0L) return null
        Draft(
            noteId = noteId,
            title = props.getProperty("title") ?: "",
            body = props.getProperty("body") ?: "",
            category = props.getProperty("category") ?: "Ideas",
            color = props.getProperty("color")?.toIntOrNull() ?: 0,
            background = props.getProperty("background")?.toIntOrNull() ?: 0,
            pinned = props.getProperty("pinned")?.toBooleanStrictOrNull() ?: false,
            reminder = props.getProperty("reminder") ?: "",
            wallpaperUri = props.getProperty("wallpaperUri") ?: "",
            savedAtMillis = props.getProperty("savedAtMillis")?.toLongOrNull() ?: 0L
        )
    }.getOrNull()

    companion object {
        private const val DIRECTORY_NAME = "drafts"
        private const val SCHEMA_VERSION = 1
        private const val TMP_SUFFIX = ".tmp"
        private const val MAX_BYTES = 2 * 1024 * 1024
    }
}