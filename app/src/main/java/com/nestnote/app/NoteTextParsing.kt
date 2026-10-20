package com.nestnote.app

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Reminder parsing and generic cursor-insertion helpers.
 *
 * Kept separate from [NoteText] so nothing in the existing editor needs to change.
 * All functions are pure and JVM-testable.
 */
object NoteTextParsing {

    /** Ordered from most specific to least specific. First successful parse wins. */
    private val REMINDER_PATTERNS: List<String> = listOf(
        "MMM d, yyyy 'at' h:mm a",
        "MMM d, yyyy, h:mm a",
        "MMM d, h:mm a",
        "MMM d, yyyy",
        "MMM d",
        "yyyy-MM-dd'T'HH:mm",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )

    /**
     * Parses a reminder string back into a [Calendar]. Returns null if the string
     * is blank, malformed, or matches no known pattern.
     *
     * Year handling: when the matched pattern has no year component, the current
     * year is inferred. If that inferred date is in the past, the year is rolled
     * forward so the reminder lands in the near future. This mirrors what a user
     * usually wants when they say "Oct 20, 9am" in December.
     */
    fun parseReminderOrNull(
        reminder: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Calendar? {
        val trimmed = reminder.trim()
        if (trimmed.isEmpty()) return null
        val locale = Locale.getDefault()
        for (pattern in REMINDER_PATTERNS) {
            val parsed = try {
                SimpleDateFormat(pattern, locale).apply { isLenient = false }.parse(trimmed)
            } catch (_: Exception) {
                null
            } ?: continue
            return adjustYearIfAbsent(parsed, pattern, nowMillis)
        }
        return null
    }

    private fun adjustYearIfAbsent(parsed: Date, pattern: String, nowMillis: Long): Calendar {
        val cal = Calendar.getInstance().apply { time = parsed }
        if (pattern.contains('y')) return cal
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        cal.set(Calendar.YEAR, nowCal.get(Calendar.YEAR))
        if (cal.timeInMillis < nowMillis) cal.add(Calendar.YEAR, 1)
        return cal
    }

    /**
     * Inserts [insertion] at [cursor] in [text] and returns (newText, newCursor).
     * Cursor is clamped to [0, text.length]. After insertion the cursor sits at
     * the end of the inserted text — convenient for "insert date at cursor" actions.
     */
    fun insertAtCursor(text: String, cursor: Int, insertion: String): Pair<String, Int> {
        val safe = cursor.coerceIn(0, text.length)
        val newText = text.substring(0, safe) + insertion + text.substring(safe)
        return newText to (safe + insertion.length)
    }
}