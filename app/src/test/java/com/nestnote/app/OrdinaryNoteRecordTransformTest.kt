package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrdinaryNoteRecordTransformTest {
    private fun note(privateNote: Boolean = false, encrypted: Boolean = false) = NestNote(
        id = 42L,
        title = "Original title",
        body = "Original body",
        category = "Ideas",
        color = 7,
        pinned = true,
        reminder = "tomorrow",
        updated = 123L,
        created = 100L,
        archived = true,
        background = 9,
        wallpaperUri = "content://wallpaper",
        format = "markdown",
        privateNote = privateNote,
        encrypted = encrypted,
        encryptedPayload = ""
    )

    @Test
    fun lockedRecordRemovesSensitiveAndPresentationFields() {
        val original = note()
        val locked = OrdinaryNoteRecordTransform.lockedRecord(original, "authenticated-ciphertext")

        assertEquals(original.id, locked.id)
        assertEquals(NoteStore.ENCRYPTED_NOTE_TITLE, locked.title)
        assertEquals("", locked.body)
        assertEquals("Encrypted", locked.category)
        assertEquals(0, locked.color)
        assertFalse(locked.pinned)
        assertEquals("", locked.reminder)
        assertEquals(0, locked.background)
        assertEquals("", locked.wallpaperUri)
        assertEquals("plain", locked.format)
        assertTrue(locked.encrypted)
        assertEquals("authenticated-ciphertext", locked.encryptedPayload)
        assertEquals(original.updated, locked.updated)
        assertEquals(original.created, locked.created)
        assertEquals(original.archived, locked.archived)
        assertFalse(locked.privateNote)
    }

    @Test(expected = IllegalArgumentException::class)
    fun privateNoteCannotBeConvertedByOrdinaryNoteTransform() {
        OrdinaryNoteRecordTransform.lockedRecord(note(privateNote = true), "ciphertext")
    }

    @Test(expected = IllegalArgumentException::class)
    fun alreadyEncryptedNoteCannotBeConvertedAgain() {
        OrdinaryNoteRecordTransform.lockedRecord(note(encrypted = true), "ciphertext")
    }
}
