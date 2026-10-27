package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteLinkSupportTest {

    @Test
    fun voiceMarkerLabelsAreNotNoteLinks() {
        // Regression: [[voice:<id>]] anchors are attachment markers, not note links.
        // Without the guard the raw marker text showed up as "Missing notes: voice:…"
        // under the note body.
        val dashed = "[[voice:3e967866-b886-4db1-abde-4724b7f2bf6c]]"
        val body = "see [[Project Plan]] and [[voice:87f8742b89c34486a8d281e5dbace545]]\n$dashed"

        assertEquals(listOf("Project Plan"), noteLinkTitles(body))
        assertTrue(isVoiceMarkerLabel("voice:87f8742b89c34486a8d281e5dbace545"))
        assertTrue(isVoiceMarkerLabel("voice:3e967866-b886-4db1-abde-4724b7f2bf6c"))
        assertTrue(isVoiceMarkerLabel("VOICE:87F8742B89C34486A8D281E5DBACE545"))
        // A real title that merely contains a colon is still a link.
        assertTrue(noteLinkTitles("quoted [[voice: a journal]]").contains("voice: a journal"))
    }

    @Test
    fun resolveNoteLinksSkipsVoiceMarkers() {
        val plan = NestNote(id = 1, title = "Project Plan", body = "", updated = 10, created = 5)
        val byTitle = notesByTitle(listOf(plan))
        val body = "lead [[Project Plan]] then [[voice:87f8742b89c34486a8d281e5dbace545]]"

        assertEquals(listOf("Project Plan"), resolveNoteLinks(body, byTitle).map { it.label })
        // Even a note literally titled "voice:<id>" is not linked through a marker.
        val tricky = NestNote(id = 2, title = "voice:87f8742b89c34486a8d281e5dbace545", body = "", updated = 20, created = 5)
        val trickyIndex = notesByTitle(listOf(plan, tricky))
        assertTrue(resolveNoteLinks(body, trickyIndex).none { it.note.id == 2L })
    }

    @Test
    fun backlinksIgnoreVoiceMarkersInOtherNotes() {
        val target = NestNote(id = 1, title = "Journal", body = "", updated = 10, created = 5)
        val realLinker = NestNote(id = 2, title = "Real", body = "reading [[Journal]] today", updated = 20, created = 5)
        val markerOnly = NestNote(id = 3, title = "Voice", body = "[[voice:87f8742b89c34486a8d281e5dbace545]]", updated = 30, created = 5)

        val backlinks = notesLinkingTo("Journal", listOf(target, realLinker, markerOnly), excludingId = 1L)
        assertEquals(listOf(2L), backlinks.map { it.id })
    }

    @Test
    fun wrapAsNoteLinkKeepsExactSelectionBounds() {
        val (text, start, end) = wrapAsNoteLink("hello world", 6, 11)
        assertEquals("hello [[world]]", text)
        assertEquals(8, start)
        assertEquals(13, end)
    }
}
