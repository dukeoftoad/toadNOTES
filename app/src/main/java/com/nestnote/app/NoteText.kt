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

    /**
     * Converts ordinary note prose into readable Markdown without changing the stored note.
     * Recognizes headings, bullets, numbered lists, and simple checklists; other lines stay intact.
     */
    fun toMarkdown(title: String, plainText: String): String {
        val output = mutableListOf<String>()
        if (title.isNotBlank()) {
            output += "# ${title.trim()}"
            output += ""
        }
        plainText.trim().lines().forEach { raw ->
            val line = raw.trim()
            when {
                line.isEmpty() -> output += ""
                line.matches(Regex("^#{1,6}\\s+.*")) -> output += line
                line.matches(Regex("^[-*•]\\s+.*")) -> output += "- " + line.replaceFirst(Regex("^[-*•]\\s+"), "")
                line.matches(Regex("^\\d+[.)]\\s+.*")) -> output += line.replaceFirst(Regex("^\\d+[.)]\\s+"), "").let { item -> "${Regex("^\\d+").find(line)?.value ?: "1"}. $item" }
                line.startsWith("☑ ") -> output += "- [x] " + line.removePrefix("☑ ")
                line.startsWith("☐ ") -> output += "- [ ] " + line.removePrefix("☐ ")
                else -> output += line
            }
        }
        return output.joinToString("\n").trimEnd() + "\n"
    }

    fun wordCount(text: String): Int =
        text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    /**
     * Time-of-day dashboard greeting. [hourOfDay] is a 24-hour Calendar.HOUR_OF_DAY value;
     * boundaries match the design reference (before noon, afternoon until 17:59, evening otherwise).
     */
    fun greetingFor(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        else -> "Good evening"
    }

    /** Rough reading time at ~200 words per minute; 0 for empty text. */
    fun readingMinutes(words: Int): Int = if (words <= 0) 0 else maxOf(1, (words + 199) / 200)

    fun lineStart(text: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, text.length)
        if (c == 0) return 0
        val lf = text.lastIndexOf('\n', c - 1)
        val cr = text.lastIndexOf('\r', c - 1)
        return maxOf(lf, cr) + 1
    }

    fun lineEnd(text: String, cursor: Int): Int {
        val c = cursor.coerceIn(0, text.length)
        val lf = text.indexOf('\n', c)
        val cr = text.indexOf('\r', c)
        return listOf(lf, cr).filter { it >= 0 }.minOrNull() ?: text.length
    }

    /** Leading whitespace of a line. Checklist views already tolerate it, so formatting actions must too. */
    private fun indentOf(line: String): String = line.takeWhile { it.isWhitespace() }

    /** The list prefix of [line], ignoring leading indentation; null for plain lines. */
    private fun prefixOf(line: String): String? {
        val content = line.substring(indentOf(line).length)
        return listPrefixes.firstOrNull { content.startsWith(it) }
            ?: Regex("""^\d+\. """).find(content)?.value
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

    /** Inserts a Markdown link; selected text becomes its label, with the URL ready to edit. */
    fun insertMarkdownLink(text: String, selectionStart: Int, selectionEnd: Int): Triple<String, Int, Int> {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(start, text.length)
        val label = if (start == end) "text" else text.substring(start, end)
        val replacement = "[$label](https://)"
        val updated = text.substring(0, start) + replacement + text.substring(end)
        val urlStart = start + label.length + 3 + "https://".length
        return Triple(updated, urlStart, urlStart)
    }

    /** Toggles a Markdown blockquote prefix on the cursor's line. */
    fun toggleBlockquote(text: String, cursor: Int): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val end = lineEnd(text, c)
        val line = text.substring(start, end)
        val contentStart = start + indentOf(line).length
        val alreadyQuoted = text.startsWith("> ", contentStart)
        val updated = if (alreadyQuoted) text.removeRange(contentStart, contentStart + 2)
            else text.substring(0, contentStart) + "> " + text.substring(contentStart)
        val nextCursor = if (alreadyQuoted && c >= contentStart + 2) c - 2
            else if (!alreadyQuoted && c >= contentStart) c + 2 else c
        return updated to nextCursor.coerceIn(0, updated.length)
    }

    /** Toggles a Markdown heading level (1–6) on the cursor's line. */
    fun toggleHeadingLevel(text: String, cursor: Int, level: Int): Pair<String, Int> {
        require(level in 1..6) { "Heading level must be between 1 and 6" }
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val end = lineEnd(text, c)
        val line = text.substring(start, end)
        val contentStart = start + indentOf(line).length
        val content = text.substring(contentStart, end)
        val existing = Regex("""^#{1,6} """).find(content)?.value.orEmpty()
        val desired = "#".repeat(level) + " "
        val replacement = if (existing == desired) "" else desired
        val next = text.substring(0, contentStart) + replacement +
            text.substring(contentStart + existing.length)
        val nextCursor = if (c >= contentStart + existing.length) {
            c + replacement.length - existing.length
        } else {
            c
        }
        return next to nextCursor.coerceIn(0, next.length)
    }

    /** Backwards-compatible level-one heading action. */
    fun toggleHeading(text: String, cursor: Int): Pair<String, Int> =
        toggleHeadingLevel(text, cursor, 1)

    /** Toggles a Markdown numbered-list prefix on the current line. */
    fun toggleNumberedList(text: String, cursor: Int): Pair<String, Int> {
        val c = cursor.coerceIn(0, text.length)
        val start = lineStart(text, c)
        val end = lineEnd(text, c)
        val line = text.substring(start, end)
        val indent = indentOf(line)
        val contentStart = start + indent.length
        val content = text.substring(contentStart, end)
        val match = Regex("""^\d+\. """).find(content)
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
            Regex("""^\d+\. """).matches(prefix) -> "${prefix.substringBefore('.').toInt() + 1}. "
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
        // Derive the actual changed range from the two texts. The returned cursor may
        // be inside a newly inserted prefix, so it is not a reliable edit-delta anchor.
        var prefix = 0
        val sharedLimit = minOf(oldText.length, newText.length)
        while (prefix < sharedLimit && oldText[prefix] == newText[prefix]) prefix++

        var oldSuffix = oldText.length
        var newSuffix = newText.length
        while (oldSuffix > prefix && newSuffix > prefix &&
            oldText[oldSuffix - 1] == newText[newSuffix - 1]
        ) {
            oldSuffix--
            newSuffix--
        }
        val delta = newSuffix - oldSuffix
        fun map(position: Int): Int {
            val mapped = when {
                position <= prefix -> position
                position >= oldSuffix -> position + delta
                else -> if (position - prefix <= oldSuffix - position) prefix else newSuffix
            }
            return mapped.coerceIn(0, newText.length)
        }
        val mappedStart = map(start)
        val mappedEnd = map(end)
        return if (selectionStart <= selectionEnd) mappedStart to mappedEnd else mappedEnd to mappedStart
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

    // --- Voice attachment markers ---
    //
    // Voice notes are anchored in the body as markers of the form
    //   [[voice:<attachmentId>]]
    // Each marker must sit on its own line so the renderer can lay out inline
    // player cards between text blocks without disturbing surrounding markdown.
    // The id itself is a UUID and never contains ']' or newlines; the regex
    // excludes both so a malformed marker cannot swallow the rest of the note.

    private val VoiceMarkerCandidateRegex = Regex("""\[\[voice:([^\]\n]+)\]\]""")
    private val VoiceAttachmentIdRegex = Regex("""(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")
    private val VoiceMarkerRegex = Regex("""\[\[voice:([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})\]\]""")

    /** Position of a voice marker in the body. [range] is the marker's character span, end-exclusive. */
    data class VoiceMarker(val attachmentId: String, val range: IntRange)

    /** A body segment, alternating between plain text and a single voice attachment. */
    data class VoiceSegment(val text: String, val attachmentId: String?)

    /** All voice markers in [body], in document order. */
    fun voiceMarkers(body: String): List<VoiceMarker> =
        VoiceMarkerRegex.findAll(body).map { VoiceMarker(it.groupValues[1], it.range) }.toList()

    /** True when [body] already references [attachmentId]. */
    fun hasVoiceMarker(body: String, attachmentId: String): Boolean =
        voiceMarkers(body).any { it.attachmentId == attachmentId }

    /**
     * Splits [body] into alternating text and voice segments.
     * Text segments preserve their original whitespace, including newlines.
     * An empty body yields one empty text segment.
     */
    fun voiceSegments(body: String): List<VoiceSegment> {
        val out = mutableListOf<VoiceSegment>()
        var cursor = 0
        VoiceMarkerCandidateRegex.findAll(body).forEach { match ->
            if (match.range.first > cursor) {
                out += VoiceSegment(body.substring(cursor, match.range.first), null)
            }
            val attachmentId = match.groupValues[1]
            if (VoiceAttachmentIdRegex.matches(attachmentId)) {
                out += VoiceSegment("", attachmentId)
            } else {
                // Never expose malformed internal marker payloads in the rendered preview.
                out += VoiceSegment("Voice note", null)
            }
            cursor = match.range.last + 1
        }
        if (cursor < body.length) out += VoiceSegment(body.substring(cursor), null)
        if (out.isEmpty()) out += VoiceSegment(body, null)
        return out
    }

    /**
     * Inserts a marker for [attachmentId] at [cursor], padding with single newlines
     * so the marker always sits on its own line. Returns the updated body and the
     * cursor position immediately after the inserted marker line.
     */
    fun insertVoiceMarker(body: String, cursor: Int, attachmentId: String): Pair<String, Int> {
        val marker = "[[voice:$attachmentId]]"
        val c = cursor.coerceIn(0, body.length)
        val needsLeadingNewline = c > 0 && body[c - 1] != '\n'
        val needsTrailingNewline = c < body.length && body[c] != '\n'
        val insertion = buildString {
            if (needsLeadingNewline) append('\n')
            append(marker)
            if (needsTrailingNewline) append('\n')
        }
        val updated = body.substring(0, c) + insertion + body.substring(c)
        return updated to (c + insertion.length)
    }

    /**
     * Removes every marker for [attachmentId]. When a marker is sandwiched between
     * newlines on both sides, one of those newlines is also removed so the result
     * does not contain a dangling blank line.
     */
    fun removeVoiceMarker(body: String, attachmentId: String): String {
    val markers = voiceMarkers(body).filter { it.attachmentId == attachmentId }
    if (markers.isEmpty()) return body
    var result = body
    markers.reversed().forEach { marker ->
        val start = marker.range.first
        val end = marker.range.last + 1
        val hasLeading = start > 0 && result[start - 1] == '\n'
        val hasTrailing = end < result.length && result[end] == '\n'
        // Remove the marker plus exactly one adjacent newline so the two
        // surrounding lines merge with a single separator instead of two.
        val trimmedStart = if (hasLeading) start - 1 else start
        val trimmedEnd = if (!hasLeading && hasTrailing) end + 1 else end
        result = result.removeRange(trimmedStart, trimmedEnd)
    }
    return result
}

    /**
     * One-time migration: appends a marker for every id in [existingIds] that is not
     * already anchored in [body]. Preserves the order of [existingIds]. Returns the
     * original instance unchanged when there is nothing to migrate.
     */
    fun migrateOrphanAttachments(body: String, existingIds: List<String>): String {
        if (existingIds.isEmpty()) return body
        val anchored = voiceMarkers(body).map { it.attachmentId }.toSet()
        val orphans = existingIds.filter { it !in anchored }
        if (orphans.isEmpty()) return body
        val sb = StringBuilder(body)
        if (sb.isNotEmpty() && sb.last() != '\n') sb.append('\n')
        orphans.forEach { id ->
            sb.append("[[voice:").append(id).append("]]\n")
        }
        return sb.toString()
    }
}
