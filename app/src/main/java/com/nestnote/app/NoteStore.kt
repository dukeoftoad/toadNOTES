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

class NoteStore(context: Context) {
    private val prefs = context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)

    fun load(): List<NestNote> {
        val raw = prefs.getString("notes", null) ?: return sampleNotes()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
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
        }.getOrElse { sampleNotes() }
    }

    fun save(notes: List<NestNote>) {
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
        prefs.edit().putString("notes", array.toString()).apply()
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
