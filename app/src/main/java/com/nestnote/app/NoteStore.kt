package com.nestnote.app

import android.content.Context
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

class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)

    fun load(): List<NestNote> {
        val raw = prefs.getString("notes", null) ?: return sampleNotes()
        return decode(raw) ?: sampleNotes()
    }

    fun save(notes: List<NestNote>) {
        prefs.edit().putString("notes", encode(notes)).apply()
    }

    companion object {
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

        /**
         * Parses a JSON array of notes. Returns null if the text is not valid.
         * With [requireFields] every record must carry at least an id and a title
         * (used to reject unrelated files during import).
         */
        fun decode(raw: String, requireFields: Boolean = false): List<NestNote>? = runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
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
        }.getOrNull()

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
