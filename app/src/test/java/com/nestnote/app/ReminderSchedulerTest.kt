package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.text.SimpleDateFormat

/**
 * Scheduler logic is mostly Android-framework glue. These tests focus on the pure
 * parts we can verify on the JVM: reminder parsing round-trips and the "past vs
 * future" decision. The AlarmManager calls themselves are covered by the existing
 * NoteTextParsingTest and by manual testing.
 */
class ReminderSchedulerTest {

    private fun makeNote(reminder: String, id: Long = 1L) = NestNote(
        id = id,
        title = "Test",
        body = "",
        reminder = reminder
    )

    @Test
    fun `blank reminder is not parseable`() {
        assertNull(NoteTextParsing.parseReminderOrNull(""))
    }

    @Test
    fun `future reminder parses to future time`() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(cal.time)
        val parsed = NoteTextParsing.parseReminderOrNull(formatted)
        assertNotNull(parsed)
        assertEquals(cal.timeInMillis, parsed!!.timeInMillis)
    }

    @Test
    fun `past reminder parses to past time`() {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(cal.time)
        val parsed = NoteTextParsing.parseReminderOrNull(formatted)
        assertNotNull(parsed)
        assertEquals(cal.timeInMillis, parsed!!.timeInMillis)
    }

    @Test
    fun `duplicate clears reminder so alarm does not double fire`() {
        val source = makeNote("Oct 20, 2099 at 9:00 AM")
        val duplicate = NoteDuplicate.duplicate(source, listOf(source), keepReminder = false)
        assertEquals("", duplicate.reminder)
    }

    @Test
    fun `duplicate keeps reminder when asked`() {
        val source = makeNote("Oct 20, 2099 at 9:00 AM")
        val duplicate = NoteDuplicate.duplicate(source, listOf(source), keepReminder = true)
        assertEquals(source.reminder, duplicate.reminder)
    }
}