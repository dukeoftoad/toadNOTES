package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NoteTextParsingTest {

    // Fixed reference point for all time-based assertions.
    // 2024-06-15 12:00:00 in the JVM default timezone (test runs in UTC on CI).
    private val nowMillis: Long = run {
        Calendar.getInstance().apply {
            set(2024, Calendar.JUNE, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun fmt(cal: Calendar, pattern: String): String =
        SimpleDateFormat(pattern, Locale.getDefault()).format(cal.time)

    // ---- Happy paths ----

    @Test
    fun `parses month day and time`() {
        val result = NoteTextParsing.parseReminderOrNull("Jun 20, 9:30 AM", nowMillis)
        assertEquals(2024, result!!.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, result.get(Calendar.MONTH))
        assertEquals(20, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(9, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, result.get(Calendar.MINUTE))
    }

    @Test
    fun `parses month day year and time`() {
        val result = NoteTextParsing.parseReminderOrNull("Dec 25, 2025, 7:00 PM", nowMillis)
        assertEquals(2025, result!!.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, result.get(Calendar.MONTH))
        assertEquals(25, result.get(Calendar.DAY_OF_MONTH))
        assertEquals(19, result.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `parses date with no year rolls forward if past`() {
        // June 1 is in the past relative to June 15 reference
        val result = NoteTextParsing.parseReminderOrNull("Jun 1, 9:00 AM", nowMillis)
        assertEquals(2025, result!!.get(Calendar.YEAR))
    }

    @Test
    fun `parses date with no year keeps current year if future`() {
        // June 20 is in the future relative to June 15 reference
        val result = NoteTextParsing.parseReminderOrNull("Jun 20, 9:00 AM", nowMillis)
        assertEquals(2024, result!!.get(Calendar.YEAR))
    }

    @Test
    fun `parses ISO date`() {
        val result = NoteTextParsing.parseReminderOrNull("2025-03-15", nowMillis)
        assertEquals(2025, result!!.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, result.get(Calendar.MONTH))
        assertEquals(15, result.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `parses ISO date with time`() {
        val result = NoteTextParsing.parseReminderOrNull("2025-03-15T14:30", nowMillis)
        assertEquals(2025, result!!.get(Calendar.YEAR))
        assertEquals(14, result.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, result.get(Calendar.MINUTE))
    }

    // ---- Failure cases ----

    @Test
    fun `blank string returns null`() {
        assertNull(NoteTextParsing.parseReminderOrNull("", nowMillis))
    }

    @Test
    fun `whitespace only returns null`() {
        assertNull(NoteTextParsing.parseReminderOrNull("   \t\n  ", nowMillis))
    }

    @Test
    fun `garbage string returns null`() {
        assertNull(NoteTextParsing.parseReminderOrNull("not a date at all", nowMillis))
    }

    @Test
    fun `malformed ISO returns null`() {
        assertNull(NoteTextParsing.parseReminderOrNull("2025-13-99", nowMillis))
    }

    // ---- Round-trip consistency ----

    @Test
    fun `format then parse round trips`() {
        // Must use the same reference year as nowMillis (2024). The pattern below
        // omits the year, so parseReminderOrNull infers it from nowMillis and rolls
        // forward if the date is already past. Using 2025 here would mismatch.
        val original = Calendar.getInstance().apply {
            set(2024, Calendar.OCTOBER, 20, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = fmt(original, "MMM d, h:mm a")
        val parsed = NoteTextParsing.parseReminderOrNull(formatted, nowMillis)
        assertEquals(original.get(Calendar.YEAR), parsed!!.get(Calendar.YEAR))
        assertEquals(original.get(Calendar.MONTH), parsed.get(Calendar.MONTH))
        assertEquals(original.get(Calendar.DAY_OF_MONTH), parsed.get(Calendar.DAY_OF_MONTH))
        assertEquals(original.get(Calendar.HOUR_OF_DAY), parsed.get(Calendar.HOUR_OF_DAY))
        assertEquals(original.get(Calendar.MINUTE), parsed.get(Calendar.MINUTE))
    }

    // ---- insertAtCursor ----

    @Test
    fun `insert at start of empty text`() {
        val (text, cursor) = NoteTextParsing.insertAtCursor("", 0, "hello")
        assertEquals("hello", text)
        assertEquals(5, cursor)
    }

    @Test
    fun `insert at end of text`() {
        val (text, cursor) = NoteTextParsing.insertAtCursor("abc", 3, "def")
        assertEquals("abcdef", text)
        assertEquals(6, cursor)
    }

    @Test
    fun `insert in middle of text`() {
        val (text, cursor) = NoteTextParsing.insertAtCursor("ac", 1, "b")
        assertEquals("abc", text)
        assertEquals(2, cursor)
    }

    @Test
    fun `insert clamps cursor above length`() {
        val (text, cursor) = NoteTextParsing.insertAtCursor("abc", 999, "X")
        assertEquals("abcX", text)
        assertEquals(4, cursor)
    }

    @Test
    fun `insert clamps cursor below zero`() {
        val (text, cursor) = NoteTextParsing.insertAtCursor("abc", -5, "X")
        assertEquals("Xabc", text)
        assertEquals(1, cursor)
    }
}