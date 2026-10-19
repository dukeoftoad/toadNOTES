package com.nestnote.app

import java.util.Locale

/**
 * Pure dashboard filtering and sorting, extracted from [NestNoteApp] so the derivation is
 * JVM-testable and the composable can recompute it only when an input actually changes
 * (see the prototype-parity audit's performance follow-up).
 *
 * View routing (kept identical to the previous inline implementation):
 * - "Archived" shows only archived notes; every other view hides them.
 * - "Home" and "All notes" show all non-archived notes.
 * - "Pinned" / "Reminders" filter by pin / non-blank reminder metadata.
 * - Any other value is treated as a category name.
 * Search matches title, body and category, case-insensitively.
 * Pinned notes always sort first, then by the selected key.
 */
internal object NoteFilters {
    fun visibleNotes(
        notes: List<NestNote>,
        selectedView: String,
        search: String,
        sortByTitle: Boolean,
        sortByCreated: Boolean
    ): List<NestNote> {
        val visible = notes.filter { note ->
            ((selectedView == "Archived" && note.archived) ||
                (selectedView != "Archived" && !note.archived && (selectedView == "Home" || selectedView == "All notes" ||
                (selectedView == "Pinned" && note.pinned) || note.category == selectedView ||
                (selectedView == "Reminders" && note.reminder.isNotBlank())))) &&
                (search.isBlank() || (note.title + " " + note.body + " " + note.category).contains(search, true))
        }
        return when {
            sortByTitle -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenBy { it.title.lowercase(Locale.getDefault()) })
            sortByCreated -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.created })
            else -> visible.sortedWith(compareByDescending<NestNote> { it.pinned }.thenByDescending { it.updated })
        }
    }
}
