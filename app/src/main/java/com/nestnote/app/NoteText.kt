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

    /** Leading whitespace of a line. Checklist views already tolerate it, so formatting actions must too. */
    private fun indentOf(line: String): String = line.takeWhile { it.isWhitespace() }

    /** The list prefix of [line], ignoring leading indentation; null for plain lines. */
    private fun prefixOf(line: String): String? {
        val content = line.substring(indentOf(line).length)
        return listPrefixes.firstOrNull { content.startsWith(it) }
            ?: Regex("""^\\d+\\. """).find(content)?.value
    }

    /** Replaces the list prefix of the cursor's line. Returns new text and new cursor. */
    private fun setPrefix(text: String, cursor: Int, newPrefix: String): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val end = lineEnd(text, c)
        val line = text.substring(start, end)
        val old = prefixOf(line).orEmpty()
        // Replace the prefix after any indentation so an indented item never gets a second prefix.
        val contentStart = start + indentOf(line).length
        val next = text.substring(0, contentStart) + newPrefix + text.substring(contentStart + old.length)
        val newCursor = (c - old.length + newPrefix.length).coerceAtLeast(contentStart + newPrefix.length)
        return next to newCursor
    }

    /**
     * Wraps the current selection in Markdown markers, or inserts an empty marker pair
     * at a collapsed cursor. Returns updated text and selection bounds.
     */
    fun toggleInlineFormat(text: String, selectionStart: Int, selectionEnd: Int, marker: String): Triple<String, Int, Int> {
        require(marker.isNotEmpty())
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(start, text.length)
        if (start == end) {
            val updated = text.substring(0, start) + marker + marker + text.substring(end)
            return Triple(updated, start + marker.length, start + marker.length)
        }
        val alreadyWrapped = start >= marker.length && end + marker.length <= text.length &&
            text.substring(start - marker.length, start) == marker &&
            text.substring(end, end + marker.length) == marker
        return if (alreadyWrapped) {
            val updated = text.removeRange(end, end + marker.length).removeRange(start - marker.length, start)
            Triple(updated, start - marker.length, end - marker.length)
        } else {
            val updated = text.substring(0, start) + marker + text.substring(start, end) + marker + text.substring(end)
            Triple(updated, start + marker.length, end + marker.length)
        }
    }

    /** Toggles a Markdown level-one heading prefix on the cursor's line. */
    fun toggleHeading(text: String, cursor: Int): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val line = text.substring(start, lineEnd(text, c))
        val contentStart = start + indentOf(line).length
        val prefix = if (text.startsWith("# ", contentStart)) "" else "# "
        val next = if (prefix.isEmpty()) {
            text.removeRange(contentStart, contentStart + 2)
        } else {
            text.substring(0, contentStart) + prefix + text.substring(contentStart)
        }
        val nextCursor = if (prefix.isEmpty() && c >= contentStart + 2) c - 2
            else if (prefix.isNotEmpty() && c >= contentStart) c + 2
            else c
        return next to nextCursor.coerceIn(0, next.length)
    }

    /** Toggles a Markdown numbered-list prefix on the current line. */
    fun toggleNumberedList(text: String, cursor: Int): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val end = lineEnd(text, c)
        val line = text.substring(start, end)
        val indent = indentOf(line)
        val contentStart = start + indent.length
        val content = text.substring(contentStart, end)
        val match = Regex("""^\\d+\\. """).find(content)
        val replacement = if (match != null) {
            content.removeRange(0, match.value.length)
        } else {
            val existing = prefixOf(line).orEmpty()
            val plain = content.removePrefix(existing)
            "1. " + plain
        }
        val updated = text.substring(0, contentStart) + replacement + text.substring(end)
        val delta = replacement.length - content.length
        return updated to (c + if (c >= contentStart) delta else 0).coerceIn(0, updated.length)
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
        val indent = indentOf(beforeCursor)
        if (beforeCursor.removePrefix(indent).removePrefix(prefix).isBlank()) {
            // Empty item: Enter ends the list instead of adding another bullet.
            return old.removeRange(start, start + indent.length + prefix.length) to start
        }
        // Keep the item's indentation so nested lists continue at the same level.
        val continuedPrefix = when {
            prefix == CHECKED -> UNCHECKED
            Regex("""^\\d+\\. """).matches(prefix) -> "${prefix.substringBefore('.').toInt() + 1}. "
            else -> prefix
        }
        val nextPrefix = indent + continuedPrefix
        return (new.substring(0, cursor) + nextPrefix + new.substring(cursor)) to (cursor + nextPrefix.length)
    }


    /**
     * Keeps a selected range anchored to the same text when a line-prefix action
     * inserts or removes characters before it. A collapsed selection uses newCursor.
     */
    fun selectionAfterLineEdit(
        oldText: String,
        newText: String,
        selectionStart: Int,
        selectionEnd: Int,
        newCursor: Int
    ): Pair<Int, Int> {
        val start = selectionStart.coerceIn(0, oldText.length)
        val end = selectionEnd.coerceIn(0, oldText.length)
        if (start == end) {
            val cursor = newCursor.coerceIn(0, newText.length)
            return cursor to cursor
        }
        val startOfLine = lineStart(oldText, start)
        val delta = newCursor - start
        fun map(position: Int): Int {
            val adjusted = if (position >= startOfLine) position + delta else position
            return adjusted.coerceIn(0, newText.length)
        }
        val mappedStart = map(start)
        val mappedEnd = map(end)
        return mappedStart to mappedEnd
    }

    data class ChecklistItem(val lineIndex: Int, val text: String, val checked: Boolean)

    /** Returns checklist items with their original line indices for interactive views. */
    fun checklistItems(text: String): List<ChecklistItem> = text.lineSequence().mapIndexedNotNull { index, line ->
        val trimmed = line.trimStart()
        when {
            trimmed.startsWith(UNCHECKED) -> ChecklistItem(index, trimmed.removePrefix(UNCHECKED), false)
            trimmed.startsWith(CHECKED) -> ChecklistItem(index, trimmed.removePrefix(CHECKED), true)
            else -> null
        }
    }.toList()

    /** Updates one checklist item's state without changing other lines or indentation. */
    fun setChecklistItemChecked(text: String, lineIndex: Int, checked: Boolean): String {
        val lines = text.split("\n").toMutableList()
        if (lineIndex !in lines.indices) return text
        val line = lines[lineIndex]
        val indentation = line.takeWhile { it.isWhitespace() }
        val content = line.drop(indentation.length)
        val prefix = when {
            content.startsWith(UNCHECKED) -> UNCHECKED
            content.startsWith(CHECKED) -> CHECKED
            else -> return text
        }
        if ((prefix == CHECKED) == checked) return text
        val replacement = if (checked) CHECKED else UNCHECKED
        lines[lineIndex] = indentation + replacement + content.removePrefix(prefix)
        return lines.joinToString("\n")
    }

    /** Returns (total checklist items, completed items) for the note body. */
    fun checklistProgress(text: String): Pair<Int, Int> {
        val items = text.lineSequence().map(String::trimStart)
            .filter { it.startsWith(UNCHECKED) || it.startsWith(CHECKED) }
            .toList()
        return items.size to items.count { it.startsWith(CHECKED) }
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

    /**
     * [laterToday], or null when that time would land on a different calendar day
     * (from 21:00 onwards), so a "Later today" preset is never offered for tomorrow.
     */
    fun laterTodayOrNull(now: Calendar): Calendar? = laterToday(now).takeIf {
        it.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            it.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
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
