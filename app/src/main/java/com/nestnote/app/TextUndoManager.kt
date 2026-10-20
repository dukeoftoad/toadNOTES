package com.nestnote.app

/**
 * Simple snapshot-based undo/redo stack with debouncing.
 *
 * Usage:
 *  - Call [push] BEFORE applying any text change. The snapshot is the "before" state.
 *  - Call [undo] with the current state to get the previous state; [undo] pushes the
 *    current state to the redo stack so [redo] can return it.
 *  - Rapid pushes within [debounceMs] coalesce into one undo step, so typing "hello"
 *    in one burst undoes as one action rather than five.
 *
 * Thread safety: not synchronized. Call from a single thread (the UI thread, in practice).
 */
class TextUndoManager(
    private val maxStackSize: Int = 100,
    private val debounceMs: Long = 300L,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    data class Snapshot(val text: String, val selectionStart: Int, val selectionEnd: Int)

    private val undoStack = ArrayDeque<Snapshot>()
    private val redoStack = ArrayDeque<Snapshot>()
    private var lastPushAt: Long = Long.MIN_VALUE

    /** Records [snapshot] as an undo point. Clears the redo stack. */
    fun push(snapshot: Snapshot) {
        val now = clock()
        val withinDebounce = now - lastPushAt < debounceMs
        lastPushAt = now
        if (withinDebounce && undoStack.isNotEmpty()) return
        undoStack.addLast(snapshot)
        while (undoStack.size > maxStackSize) undoStack.removeFirst()
        redoStack.clear()
    }

    /**
     * Returns the state to restore, or null if there is nothing to undo. [current]
     * is pushed to the redo stack so [redo] can return to it.
     */
    fun undo(current: Snapshot): Snapshot? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        // Treat the next push as a fresh burst rather than coalescing with pre-undo typing.
        lastPushAt = Long.MIN_VALUE
        return previous
    }

    /** Returns the state to restore, or null if there is nothing to redo. */
    fun redo(current: Snapshot): Snapshot? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        lastPushAt = Long.MIN_VALUE
        return next
    }

    fun canUndo(): Boolean = undoStack.isNotEmpty()

    fun canRedo(): Boolean = redoStack.isNotEmpty()

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        lastPushAt = Long.MIN_VALUE
    }

    /** Test-only visibility into stack sizes. */
    internal fun undoDepth(): Int = undoStack.size
    internal fun redoDepth(): Int = redoStack.size
}