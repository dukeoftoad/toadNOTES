package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Test

class OrdinaryNoteSelectionTest {
    private fun note(
        id: Long,
        privateNote: Boolean = false,
        encrypted: Boolean = false
    ) = NestNote(id = id, title = "note-$id", body = "body-$id",
        privateNote = privateNote, encrypted = encrypted)

    @Test
    fun encryptionSelectionExcludesPrivateAndAlreadyEncryptedNotes() {
        val notes = listOf(note(1), note(2, encrypted = true), note(3, privateNote = true))
        assertEquals(listOf(1L), OrdinaryNoteSelection.forEncryption(notes, setOf(1, 2, 3)).map { it.id })
    }

    @Test
    fun allEncryptionEligibilityExcludesPrivateAndAlreadyEncryptedNotes() {
        val notes = listOf(note(1), note(2, encrypted = true), note(3, privateNote = true))
        assertEquals(listOf(1L), OrdinaryNoteSelection.eligibleForEncryption(notes).map { it.id })
    }

    @Test
    fun decryptionSelectionIncludesOnlyEncryptedOrdinaryNotes() {
        val notes = listOf(note(1), note(2, encrypted = true), note(3, privateNote = true, encrypted = true))
        assertEquals(listOf(2L), OrdinaryNoteSelection.forDecryption(notes, setOf(1, 2, 3)).map { it.id })
    }

    @Test
    fun selectionPreservesOriginalNoteOrder() {
        val notes = listOf(note(1), note(2), note(3))
        assertEquals(listOf(1L, 3L), OrdinaryNoteSelection.forEncryption(notes, setOf(3, 1)).map { it.id })
    }

    @Test
    fun replacementsChangeOnlyMatchingIdsAndPreserveOrder() {
        val notes = listOf(note(1), note(2), note(3))
        val replacement = note(2, encrypted = true)
        val result = OrdinaryNoteSelection.applyReplacements(notes, mapOf(2L to replacement))
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id })
        // Compare against the original instances: constructing a fresh NestNote can differ in
        // timestamp defaults and makes this a flaky/incorrect equality assertion.
        assertEquals(notes[0], result[0])
        assertEquals(replacement, result[1])
        assertEquals(notes[2], result[2])
    }

    @Test
    fun emptyReplacementMapLeavesNotesUnchanged() {
        val notes = listOf(note(1), note(2))
        assertEquals(notes, OrdinaryNoteSelection.applyReplacements(notes, emptyMap()))
    }
}
