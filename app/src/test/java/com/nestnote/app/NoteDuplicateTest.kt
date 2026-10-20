package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteDuplicateTest {

    private val source = NestNote(
        id = 100L,
        title = "Grocery list",
        body = "Milk\nEggs\nBread",
        category = "Personal",
        color = 3,
        pinned = true,
        reminder = "Oct 20, 9:00 AM",
        updated = 1000L,
        created = 500L,
        archived = true,
        background = 2,
        wallpaperUri = "content://wallpaper/123"
    )

    @Test
    fun `duplicate gets different ID`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source), nowMillis = 2000L)
        assertNotEquals(source.id, copy.id)
    }

    @Test
    fun `duplicate preserves body`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source))
        assertEquals(source.body, copy.body)
    }

    @Test
    fun `duplicate preserves title`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source))
        assertEquals(source.title, copy.title)
    }

    @Test
    fun `duplicate preserves category and visual fields`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source))
        assertEquals(source.category, copy.category)
        assertEquals(source.color, copy.color)
        assertEquals(source.background, copy.background)
        assertEquals(source.wallpaperUri, copy.wallpaperUri)
    }

    @Test
    fun `duplicate clears archived`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source))
        assertFalse(copy.archived)
    }

    @Test
    fun `duplicate clears pinned by default`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source))
        assertFalse(copy.pinned)
    }

    @Test
    fun `duplicate keeps pinned when asked`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source), keepPinned = true)
        assertTrue(copy.pinned)
    }

    @Test
    fun `duplicate clears reminder by default`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source))
        assertEquals("", copy.reminder)
    }

    @Test
    fun `duplicate keeps reminder when asked`() {
        val copy = NoteDuplicate.duplicate(source, listOf(source), keepReminder = true)
        assertEquals(source.reminder, copy.reminder)
    }

    @Test
    fun `duplicate sets new timestamps`() {
        val now = 9999L
        val copy = NoteDuplicate.duplicate(source, listOf(source), nowMillis = now)
        assertEquals(now, copy.updated)
        assertEquals(now, copy.created)
    }

    @Test
    fun `duplicate ID does not collide with source collection`() {
        val source2 = source.copy(id = 200L)
        val collection = listOf(source, source2)
        val copy = NoteDuplicate.duplicate(source, collection, nowMillis = 100L)
        assertTrue(collection.none { it.id == copy.id })
    }

    @Test
    fun `duplicate of empty body note works`() {
        val empty = source.copy(body = "")
        val copy = NoteDuplicate.duplicate(empty, listOf(empty))
        assertEquals("", copy.body)
    }
}