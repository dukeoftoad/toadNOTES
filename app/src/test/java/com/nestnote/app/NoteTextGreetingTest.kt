package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteTextGreetingTest {
    @Test
    fun greetingFollowsPrototypeBoundaries() {
        // Morning: 05:00–11:59
        assertEquals("Good morning", NoteText.greetingFor(5))
        assertEquals("Good morning", NoteText.greetingFor(9))
        assertEquals("Good morning", NoteText.greetingFor(11))
        // Afternoon: 12:00–17:59
        assertEquals("Good afternoon", NoteText.greetingFor(12))
        assertEquals("Good afternoon", NoteText.greetingFor(15))
        assertEquals("Good afternoon", NoteText.greetingFor(17))
        // Evening: 18:00–04:59 (late night belongs to evening)
        assertEquals("Good evening", NoteText.greetingFor(18))
        assertEquals("Good evening", NoteText.greetingFor(21))
        assertEquals("Good evening", NoteText.greetingFor(23))
        assertEquals("Good evening", NoteText.greetingFor(0))
        assertEquals("Good evening", NoteText.greetingFor(4))
    }
}
