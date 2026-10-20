package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUndoManagerTest {

    private class FakeClock(var now: Long = 0L) {
        fun advance(ms: Long) { now += ms }
        fun get(): Long = now
    }

    private fun snapshot(text: String) = TextUndoManager.Snapshot(text, text.length, text.length)

    @Test
    fun `initial state cannot undo or redo`() {
        val m = TextUndoManager()
        assertFalse(m.canUndo())
        assertFalse(m.canRedo())
    }

    @Test
    fun `undo on empty returns null`() {
        val m = TextUndoManager()
        assertNull(m.undo(snapshot("a")))
    }

    @Test
    fun `redo on empty returns null`() {
        val m = TextUndoManager()
        assertNull(m.redo(snapshot("a")))
    }

    @Test
    fun `push then undo returns pushed snapshot`() {
        val m = TextUndoManager(debounceMs = 0L)
        m.push(snapshot("before"))
        val result = m.undo(snapshot("after"))
        assertEquals("before", result!!.text)
    }

    @Test
    fun `undo then redo restores original`() {
        val m = TextUndoManager(debounceMs = 0L)
        m.push(snapshot("A"))
        m.undo(snapshot("B"))
        val result = m.redo(snapshot("A"))
        assertEquals("B", result!!.text)
    }

    @Test
    fun `push clears redo stack`() {
        val m = TextUndoManager(debounceMs = 0L)
        m.push(snapshot("A"))
        m.undo(snapshot("B"))
        assertTrue(m.canRedo())
        m.push(snapshot("C"))
        assertFalse(m.canRedo())
    }

    @Test
    fun `rapid pushes within debounce coalesce`() {
        val clock = FakeClock()
        val m = TextUndoManager(debounceMs = 300L, clock = clock::get)
        m.push(snapshot("0"))
        clock.advance(100)
        m.push(snapshot("1"))
        clock.advance(100)
        m.push(snapshot("2"))
        // All three coalesced; only the first is on the undo stack
        assertEquals(1, m.undoDepth())
    }

    @Test
    fun `pushes spaced beyond debounce produce separate undo points`() {
        val clock = FakeClock()
        val m = TextUndoManager(debounceMs = 300L, clock = clock::get)
        m.push(snapshot("0"))
        clock.advance(400)
        m.push(snapshot("1"))
        clock.advance(400)
        m.push(snapshot("2"))
        assertEquals(3, m.undoDepth())
    }

    @Test
    fun `multi-level undo walks back through history`() {
        val clock = FakeClock()
        val m = TextUndoManager(debounceMs = 100L, clock = clock::get)
        m.push(snapshot("A"))
        clock.advance(200)
        m.push(snapshot("B"))
        clock.advance(200)
        m.push(snapshot("C"))

        assertEquals("C", m.undo(snapshot("D"))!!.text)
        assertEquals("B", m.undo(snapshot("C"))!!.text)
        assertEquals("A", m.undo(snapshot("B"))!!.text)
        assertNull(m.undo(snapshot("A")))
    }

    @Test
    fun `multi-level redo replays undone actions`() {
        val clock = FakeClock()
        val m = TextUndoManager(debounceMs = 100L, clock = clock::get)
        m.push(snapshot("A"))
        clock.advance(200)
        m.push(snapshot("B"))
        clock.advance(200)
        m.push(snapshot("C"))

        // Walk back: D → C → B. After both undos, user is at "B" with redo stack
        // containing (bottom-to-top) [D, C].
        assertEquals("C", m.undo(snapshot("D"))!!.text)
        assertEquals("B", m.undo(snapshot("C"))!!.text)

        // Walk forward: B → C → D.
        assertEquals("C", m.redo(snapshot("B"))!!.text)
        assertEquals("D", m.redo(snapshot("C"))!!.text)

        // Nothing left to redo.
        assertNull(m.redo(snapshot("D")))
    }

    @Test
    fun `stack overflow drops oldest entries`() {
        val m = TextUndoManager(maxStackSize = 3, debounceMs = 0L)
        for (i in 0 until 5) {
            m.push(snapshot("s$i"))
        }
        assertEquals(3, m.undoDepth())
        // Oldest two were dropped; undo should walk back s4 -> s3 -> s2
        assertEquals("s4", m.undo(snapshot("s5"))!!.text)
        assertEquals("s3", m.undo(snapshot("s4"))!!.text)
        assertEquals("s2", m.undo(snapshot("s3"))!!.text)
        assertNull(m.undo(snapshot("s2")))
    }

    @Test
    fun `clear empties history`() {
        val m = TextUndoManager(debounceMs = 0L)
        m.push(snapshot("A"))
        m.push(snapshot("B"))
        m.clear()
        assertFalse(m.canUndo())
        assertFalse(m.canRedo())
    }

    @Test
    fun `undo preserves selection`() {
        val m = TextUndoManager(debounceMs = 0L)
        val original = TextUndoManager.Snapshot("hello", 2, 4)
        m.push(original)
        val result = m.undo(snapshot("hello world"))
        assertEquals(2, result!!.selectionStart)
        assertEquals(4, result.selectionEnd)
    }
}