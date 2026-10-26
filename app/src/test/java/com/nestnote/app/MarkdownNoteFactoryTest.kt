package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownNoteFactoryTest {
    @Test
    fun createsNotesInInputOrderWithSelectedCategoryAndSequentialTimestamps() {
        val existing = listOf(NestNote(id = 100L, title = "Existing", body = ""))
        val parsed = listOf(
            MarkdownNoteParser.ParsedNote("First", "one"),
            MarkdownNoteParser.ParsedNote("Second", "two")
        )

        val created = MarkdownNoteFactory.create(parsed, existing, "Work", now = 100L)

        assertEquals(listOf("First", "Second"), created.map { it.title })
        assertEquals(listOf("one", "two"), created.map { it.body })
        assertEquals(listOf("Work", "Work"), created.map { it.category })
        assertEquals(listOf(100L, 101L), created.map { it.created })
        assertEquals(listOf(100L, 101L), created.map { it.updated })
        assertEquals(2, created.map { it.id }.toSet().size)
        assertTrue(created.none { it.id == existing.single().id })
    }

    @Test
    fun emptyInputCreatesNoNotes() {
        assertTrue(MarkdownNoteFactory.create(emptyList(), emptyList(), "Ideas", now = 500L).isEmpty())
    }
}
