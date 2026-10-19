package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteEditorTest {
    @Test
    fun collapsibleSlotHeightClampsNegativeSpringOvershoot() {
        assertEquals(0, collapsibleSlotHeight(958, -0.002f))
    }

    @Test
    fun collapsibleSlotHeightPreservesVisibleAndCollapsedSizes() {
        assertEquals(120, collapsibleSlotHeight(120, 1f))
        assertEquals(0, collapsibleSlotHeight(120, 0f))
    }
}