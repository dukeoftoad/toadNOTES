package com.nestnote.app

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class DraftStoreTest {

    private lateinit var root: File
    private lateinit var store: DraftStore

    @Before
    fun setUp() {
        root = Files.createTempDirectory("draftstore-test").toFile()
        store = DraftStore(root)
    }

    @After
    fun tearDown() {
        root.deleteRecursively()
    }

    private fun sampleDraft(id: Long = 1L) = DraftStore.Draft(
        noteId = id,
        title = "Test title",
        body = "Test body",
        category = "Personal",
        color = 2,
        background = 1,
        pinned = true,
        reminder = "Oct 20, 9:00 AM",
        wallpaperUri = "",
        savedAtMillis = 123L
    )

    @Test
    fun `write then read round trips`() {
        val draft = sampleDraft()
        assertTrue(store.write(draft))
        val read = store.read(draft.noteId)
        assertNotNull(read)
        assertEquals(draft, read)
    }

    @Test
    fun `read missing returns null`() {
        assertNull(store.read(999L))
    }

    @Test
    fun `read negative ID returns null`() {
        assertNull(store.read(-1L))
    }

    @Test
    fun `write negative ID returns false`() {
        assertFalse(store.write(sampleDraft(id = -1L)))
    }

    @Test
    fun `write twice overwrites`() {
        val first = sampleDraft().copy(title = "First")
        val second = sampleDraft().copy(title = "Second")
        assertTrue(store.write(first))
        assertTrue(store.write(second))
        assertEquals("Second", store.read(first.noteId)?.title)
    }

    @Test
    fun `clear removes draft`() {
        val draft = sampleDraft()
        assertTrue(store.write(draft))
        assertTrue(store.clear(draft.noteId))
        assertNull(store.read(draft.noteId))
    }

    @Test
    fun `clear missing returns true`() {
        assertTrue(store.clear(999L))
    }

    @Test
    fun `listAll returns all drafts`() {
        store.write(sampleDraft(id = 1L))
        store.write(sampleDraft(id = 2L))
        store.write(sampleDraft(id = 3L))
        val all = store.listAll()
        assertEquals(3, all.size)
        assertTrue(all.map { it.noteId }.containsAll(listOf(1L, 2L, 3L)))
    }

    @Test
    fun `clearAll removes all drafts`() {
        store.write(sampleDraft(id = 1L))
        store.write(sampleDraft(id = 2L))
        assertEquals(2, store.clearAll())
        assertNull(store.read(1L))
        assertNull(store.read(2L))
    }

    @Test
    fun `corrupted content returns null and does not throw`() {
        val draft = sampleDraft()
        store.write(draft)
        File(root, "note-${draft.noteId}.json").writeText("not a properties file {{{", Charsets.UTF_8)
        assertNull(store.read(draft.noteId))
    }

    @Test
    fun `unsupported schema version returns null`() {
        val draft = sampleDraft()
        store.write(draft)
        val file = File(root, "note-${draft.noteId}.json")
        // Properties format writes each entry on its own line as key=value.
        file.writeText(
            file.readText().replace("schemaVersion=1", "schemaVersion=999"),
            Charsets.UTF_8
        )
        assertNull(store.read(draft.noteId))
    }

    @Test
    fun `oversized file returns null`() {
        val draft = sampleDraft()
        store.write(draft)
        File(root, "note-${draft.noteId}.json").writeText("x".repeat(3 * 1024 * 1024))
        assertNull(store.read(draft.noteId))
    }
}