package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownNoteParserTest {
    @Test
    fun firstLevelHeadingBecomesTitleAndIsRemovedFromBody() {
        val parsed = MarkdownNoteParser.parse("# Trip notes\nPack bags\n\n# Later heading\nKeep this", "trip.md")
        assertEquals("Trip notes", parsed.title)
        assertEquals("Pack bags\n\n# Later heading\nKeep this", parsed.body)
    }

    @Test
    fun missingHeadingUsesFallbackFileNameAndKeepsTrimmedBody() {
        val parsed = MarkdownNoteParser.parse("  Just some text\n ", "journal")
        assertEquals("journal", parsed.title)
        assertEquals("Just some text", parsed.body)
    }

    @Test
    fun missingHeadingAndBlankFallbackUseImportedNoteTitle() {
        val parsed = MarkdownNoteParser.parse("  Body  ", " ")
        assertEquals("Imported note", parsed.title)
        assertEquals("Body", parsed.body)
    }

    @Test
    fun blankFirstHeadingUsesFallbackTitle() {
        val parsed = MarkdownNoteParser.parse("#   \nBody", "notes-file")
        assertEquals("notes-file", parsed.title)
        assertEquals("Body", parsed.body)
    }

    @Test
    fun emptyDocumentUsesFallbackTitleAndEmptyBody() {
        val parsed = MarkdownNoteParser.parse("", "")
        assertEquals("Imported note", parsed.title)
        assertEquals("", parsed.body)
    }
}
