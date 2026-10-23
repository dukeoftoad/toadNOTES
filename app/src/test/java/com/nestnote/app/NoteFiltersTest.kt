package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteFiltersTest {
    private val work = NestNote(id = 1, title = "Launch checklist", body = "ship it", category = "Work", pinned = true, updated = 100, created = 50)
    private val ideas = NestNote(id = 2, title = "App ideas", body = "widget thoughts", category = "Ideas", updated = 300, created = 200)
    private val personal = NestNote(id = 3, title = "Piano", body = "practice C and G", category = "Personal", reminder = "2026-01-01 09:00", updated = 200, created = 150)
    private val archived = NestNote(id = 4, title = "Old plan", body = "done", category = "Work", archived = true, updated = 400, created = 10)
    private val all = listOf(work, ideas, personal, archived)

    @Test
    fun homeShowsAllNonArchivedNotes() {
        val visible = NoteFilters.visibleNotes(all, "Home", "", false, false)
        assertEquals(listOf(ideas, personal, work).sortedBy { it.id }, visible.sortedBy { it.id })
    }

    @Test
    fun archivedViewShowsOnlyArchivedNotes() {
        val visible = NoteFilters.visibleNotes(all, "Archived", "", false, false)
        assertEquals(listOf(archived), visible)
    }

    @Test
    fun categoryViewFiltersByCategoryAndHidesArchived() {
        val visible = NoteFilters.visibleNotes(all, "Work", "", false, false)
        assertEquals(listOf(work), visible)
    }

    @Test
    fun pinnedViewShowsOnlyPinnedActiveNotes() {
        val visible = NoteFilters.visibleNotes(all, "Pinned", "", false, false)
        assertEquals(listOf(work), visible)
    }

    @Test
    fun remindersViewRequiresReminderMetadata() {
        val visible = NoteFilters.visibleNotes(all, "Reminders", "", false, false)
        assertEquals(listOf(personal), visible)
    }

    @Test
    fun searchFiltersTitleBodyAndCategoryCaseInsensitively() {
        assertEquals(listOf(work), NoteFilters.visibleNotes(all, "Home", "launch", false, false))
        assertEquals(listOf(ideas), NoteFilters.visibleNotes(all, "Home", "WIDGET", false, false))
        assertEquals(listOf(personal), NoteFilters.visibleNotes(all, "Home", "personal", false, false))
        // Blank query never filters.
        assertEquals(3, NoteFilters.visibleNotes(all, "Home", "   ", false, false).size)
    }

    @Test
    fun privateNotesAreExcludedFromHomeAndOnlyAppearInPrivateView() {
        val secret = all.first().copy(id = 999L, title = "Secret", privateNote = true)
        assertFalse(NoteFilters.visibleNotes(all + secret, "Home", "", false, false).any { it.id == secret.id })
        assertTrue(NoteFilters.visibleNotes(all + secret, "Private", "", false, false).any { it.id == secret.id })
    }

    @Test
    fun searchAppliesWithinTheSelectedView() {
        val visible = NoteFilters.visibleNotes(all, "Work", "ship", false, false)
        assertEquals(listOf(work), visible)
        assertTrue(NoteFilters.visibleNotes(all, "Work", "piano", false, false).isEmpty())
    }

    @Test
    fun defaultSortPinsFirstThenMostRecentlyEdited() {
        val visible = NoteFilters.visibleNotes(all, "Home", "", false, false)
        assertEquals(work, visible.first())
        assertEquals(listOf(300L, 200L), visible.drop(1).map { it.updated })
    }

    @Test
    fun createdSortPinsFirstThenMostRecentlyCreated() {
        val visible = NoteFilters.visibleNotes(all, "Home", "", false, true)
        assertEquals(work, visible.first())
        assertEquals(listOf(200L, 150L), visible.drop(1).map { it.created })
    }

    @Test
    fun titleSortPinsFirstThenAlphabetical() {
        val visible = NoteFilters.visibleNotes(all, "Home", "", true, false)
        assertEquals(work, visible.first())
        assertEquals(listOf("App ideas", "Piano"), visible.drop(1).map { it.title })
    }
}
