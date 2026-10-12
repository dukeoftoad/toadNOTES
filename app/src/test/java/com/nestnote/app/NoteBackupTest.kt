package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteBackupTest {
    private fun note(id: Long, title: String, updated: Long = 100L) =
        NestNote(id = id, title = title, body = "body $id", category = "Work", color = 2, pinned = true,
            reminder = "2026-10-14 09:00", updated = updated, created = 50L, archived = true, background = 3)

    @Test
    fun encodeThenDecodeRoundTripsEveryField() {
        val notes = listOf(note(1, "One"), note(2, "Two"))
        val decoded = NoteStore.decode(NoteStore.encode(notes), requireFields = true)
        assertEquals(notes, decoded)
    }

    @Test
    fun decodeRejectsInvalidAndUnrelatedJson() {
        assertNull(NoteStore.decode("not json"))
        assertNull(NoteStore.decode("{\"a\":1}"))
        assertNull(NoteStore.decode("[{\"foo\":\"bar\"}]", requireFields = true))
        assertNotNull(NoteStore.decode("[]", requireFields = true))
    }

    @Test
    fun mergeAddsNewNotesAndKeepsTheNewerCopy() {
        val existing = listOf(note(1, "Local old", updated = 100), note(2, "Local new", updated = 500))
        val incoming = listOf(note(1, "Backup newer", updated = 300), note(2, "Backup older", updated = 200), note(3, "Brand new"))
        val result = NoteStore.merge(existing, incoming)
        assertEquals(1, result.added)
        assertEquals(1, result.updated)
        assertEquals(listOf("Backup newer", "Local new", "Brand new"), result.notes.map { it.title })
    }

    @Test
    fun mergeIntoEmptyKeepsBackupOrder() {
        val result = NoteStore.merge(emptyList(), listOf(note(5, "A"), note(4, "B")))
        assertEquals(2, result.added)
        assertTrue(result.notes.map { it.id } == listOf(5L, 4L))
    }
}
