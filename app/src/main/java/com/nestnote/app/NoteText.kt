package com.nestnote.app

import java.util.Calendar
import java.util.Locale

/**
 * Pure text helpers for the note editor (checklists, bullets, counters).
 * Kept free of Compose/Android types so they can be unit tested on the JVM.
 */
object NoteText {
    const val UNCHECKED = "☐ "
    const val CHECKED = "☑ "
    const val BULLET = "• "

    private val listPrefixes = listOf(CHECKED, UNCHECKED, BULLET)

    fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    /** Rough reading time at ~200 words per minute; 0 for empty text. */
    fun readingMinutes(words: Int): Int = if (words <= 0) 0 else maxOf(1, (words + 199) / 200)

    fun lineStart(text: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, text.length)
        return if (c == 0) 0 else text.lastIndexOf('\n', c - 1) + 1
    }

    fun lineEnd(text: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, text.length)
        val i = text.indexOf('\n', c)
        return if (i < 0) text.length else i
    }

    private fun prefixOf(line: String): String? = listPrefixes.firstOrNull { line.startsWith(it) }

    /** Replaces the list prefix of the cursor's line. Returns new text and new cursor. */
    private fun setPrefix(text: String, cursor: Int, newPrefix: String): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val end = lineEnd(text, c)
        val old = prefixOf(text.substring(start, end)).orEmpty()
        val next = text.substring(0, start) + newPrefix + text.substring(start + old.length)
        val newCursor = (c - old.length + newPrefix.length).coerceAtLeast(start + newPrefix.length)
        return next to newCursor
    }

    /** Cycles the current line: plain → ☐ → ☑ → plain. */
    fun cycleChecklist(text: String, cursor: Int): Pair<String, Int> {
        val line = text.substring(lineStart(text, cursor), lineEnd(text, cursor))
        return when (prefixOf(line)) {
            UNCHECKED -> setPrefix(text, cursor, CHECKED)
            CHECKED -> setPrefix(text, cursor, "")
            else -> setPrefix(text, cursor, UNCHECKED)
        }
    }

    /** Toggles a bullet on the current line (replaces a checkbox if present). */
    fun toggleBullet(text: String, cursor: Int): Pair<String, Int> {
        val line = text.substring(lineStart(text, cursor), lineEnd(text, cursor))
        return if (prefixOf(line) == BULLET) setPrefix(text, cursor, "") else setPrefix(text, cursor, BULLET)
    }

    /**
     * Called after the user typed a single newline. If the line they just finished
     * was a list item, continues the list (or ends it when the item was empty).
     * Returns null when nothing should change.
     */
    fun continueList(old: String, new: String, cursor: Int): Pair<String, Int>? {
        if (new.length != old.length + 1 || cursor < 1 || cursor > new.length || new[cursor - 1] != '\n') return null
        if (new.removeRange(cursor - 1, cursor) != old) return null
        val start = lineStart(old, cursor - 1)
        val beforeCursor = old.substring(start, cursor - 1)
        val prefix = prefixOf(beforeCursor) ?: return null
        if (beforeCursor.removePrefix(prefix).isBlank()) {
            // Empty item: Enter ends the list instead of adding another bullet.
            return old.removeRange(start, start + prefix.length) to start
        }
        val nextPrefix = if (prefix == CHECKED) UNCHECKED else prefix
        return (new.substring(0, cursor) + nextPrefix + new.substring(cursor)) to (cursor + nextPrefix.length)
    }

    fun shareText(title: String, body: String): String =
        listOf(title.trim(), body.trim()).filter { it.isNotEmpty() }.joinToString("\n\n")

    // --- Reminder helpers (metadata only; no alarms are scheduled) ---

    fun formatReminder(cal: Calendar): String = String.format(
        Locale.US, "%04d-%02d-%02d %02d:%02d",
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH),
        cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE)
    )

    /** Three hours from now, on the hour. */
    fun laterToday(now: Calendar): Calendar = (now.clone() as Calendar).apply {
        add(Calendar.HOUR_OF_DAY, 3)
        set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    /** Tomorrow at 09:00. */
    fun tomorrowMorning(now: Calendar): Calendar = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }

    /** The next Monday (never today) at 09:00. */
    fun nextMonday(now: Calendar): Calendar = (now.clone() as Calendar).apply {
        val ahead = (Calendar.MONDAY - get(Calendar.DAY_OF_WEEK) + 7) % 7
        add(Calendar.DAY_OF_YEAR, if (ahead == 0) 7 else ahead)
        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
}
