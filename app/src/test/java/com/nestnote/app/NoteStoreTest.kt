package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteStoreTest {
    private lateinit var context: Context
    private lateinit var store: NoteStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = NoteStore(context)
    }

    @Test
    fun firstLoadReturnsStarterNotes() {
        val notes = store.load()
        assertTrue(notes.isNotEmpty())
        assertTrue(notes.any { it.title == "Project Plan" })
    }

    @Test
    fun saveAndLoadRoundTripsAllNoteFields() {
        val expected = listOf(NestNote(
            id = 42L, title = "Test title", body = "Body text",
            category = "Work", color = 3, pinned = true,
            reminder = "2026-10-12 09:30", updated = 123456L
        ))
        store.save(expected)
        assertEquals(expected, store.load())
    }

    @Test
    fun savingEmptyListPersistsAnEmptyList() {
        store.save(emptyList())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun malformedStoredJsonFallsBackToStarterNotes() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString("notes", "{not valid json").commit()
        val notes = store.load()
        assertFalse(notes.isEmpty())
        assertTrue(notes.any { it.title == "Project Plan" })
    }
}
