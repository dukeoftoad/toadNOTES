package com.nestnote.app

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale

/** Dashboard filtering, including a small local-only query language for power users. */
internal object NoteFilters {
    private data class Query(
        val terms: List<String>,
        val category: String? = null,
        val pinned: Boolean? = null,
        val archived: Boolean = false,
        val reminder: Boolean = false,
        val voice: Boolean = false,
        val after: Long? = null,
        val before: Long? = null
    )

    private val filterToken = Regex("(?:^|\\s)(is|has|category|in|after|before):([^\\s]+)", RegexOption.IGNORE_CASE)

    fun visibleNotes(notes: List<NestNote>, selectedView: String, search: String, sortByTitle: Boolean, sortByCreated: Boolean): List<NestNote> {
        val query = parseQuery(search)
        val showArchived = selectedView == "Archived" || query.archived
        val terms = query.terms.map { it.lowercase(Locale.ROOT) }
        val visible = notes.filter { note ->
            val inView = if (selectedView == "Private") note.privateNote && !note.archived
            else if (showArchived) note.archived && !note.privateNote else !note.archived && !note.privateNote && when (selectedView) {
                "Home", "All notes" -> true
                "Pinned" -> note.pinned
                "Reminders" -> note.reminder.isNotBlank()
                else -> note.category.equals(selectedView, ignoreCase = true)
            }
            // Reject notes outside the selected view before allocating/lowercasing their bodies.
            if (!inView) return@filter false
            if (query.category != null && !note.category.equals(query.category, ignoreCase = true)) return@filter false
            if (query.pinned == true && !note.pinned) return@filter false
            if (query.reminder && note.reminder.isBlank()) return@filter false
            if (query.after != null && note.updated < query.after) return@filter false
            if (query.before != null && note.updated >= query.before) return@filter false
            if (query.voice && NoteText.voiceMarkers(note.body).isEmpty()) return@filter false
            if (terms.isEmpty()) return@filter true
            val searchable = "${note.title} ${note.body} ${note.category}".lowercase(Locale.ROOT)
            terms.all(searchable::contains)
        }
        return when {
            sortByTitle -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
            sortByCreated -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.created })
            else -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.updated })
        }
    }

    private fun parseQuery(raw: String): Query {
        var category: String? = null
        var pinned: Boolean? = null
        var archived = false
        var reminder = false
        var voice = false
        var after: Long? = null
        var before: Long? = null
        val consumed = mutableListOf<IntRange>()
        filterToken.findAll(raw).forEach { match ->
            val operator = match.groupValues[1].lowercase(Locale.ROOT)
            val value = match.groupValues[2]
            val parsedDate = if (operator == "after" || operator == "before") parseDate(value) else null
            val recognized = when (operator) {
                "is" -> when (value.lowercase(Locale.ROOT)) {
                    "pinned" -> { pinned = true; true }
                    "archived" -> { archived = true; true }
                    else -> false
                }
                "has" -> when (value.lowercase(Locale.ROOT)) {
                    "reminder" -> { reminder = true; true }
                    "voice", "audio", "attachment" -> { voice = true; true }
                    else -> false
                }
                "category", "in" -> { category = value.replace('_', ' '); true }
                "after" -> if (parsedDate != null) { after = parsedDate; true } else false
                "before" -> if (parsedDate != null) { before = parsedDate; true } else false
                else -> false
            }
            if (recognized) consumed += match.range
        }
        val remaining = buildString {
            var cursor = 0
            consumed.forEach { range ->
                append(raw.substring(cursor, range.first))
                cursor = range.last + 1
            }
            append(raw.substring(cursor))
        }
        return Query(remaining.trim().split(Regex("\\s+")).filter { it.isNotBlank() }, category, pinned, archived, reminder, voice, after, before)
    }

    private fun parseDate(value: String): Long? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }
        val position = ParsePosition(0)
        val date = formatter.parse(value, position) ?: return null
        if (position.index != value.length) return null
        val calendar = java.util.Calendar.getInstance().apply {
            time = date
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return calendar.timeInMillis
    }
}
