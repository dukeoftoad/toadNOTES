package com.nestnote.app

/**
 * Aligns Markdown pipe tables after a multi-character paste.
 *
 * Only blocks with a valid GFM delimiter row are touched. Fenced code and ordinary
 * pipe-containing prose are left unchanged. Delimiters accept the full GFM form
 * (`:-:`, `:-`, `-:`, `-`), escaped pipes (`\|`) never split cells, and rows written
 * without outer pipes (`A | B`) are recognized just like piped rows. This formatter
 * changes whitespace only; it never changes cell contents or the stored table's
 * Markdown semantics.
 */
internal object MarkdownTableFormatter {
    fun format(text: String): String {
        val lines = text.split("\n").toMutableList()
        var index = 0
        var inFence = false
        var fenceMarker = ""
        while (index < lines.size) {
            val trimmed = lines[index].trimStart()
            val fence = trimmed.startsWith("```") || trimmed.startsWith("~~~")
            if (fence) {
                val marker = trimmed.take(3)
                if (!inFence) {
                    inFence = true
                    fenceMarker = marker
                } else if (marker == fenceMarker) {
                    inFence = false
                    fenceMarker = ""
                }
                index++
                continue
            }
            if (inFence || index + 1 >= lines.size || !isPipeRow(lines[index]) ||
                !isSeparatorRow(lines[index + 1]) ||
                // GFM requires the delimiter row to match the header's cell count; without
                // this check prose like "A | B" above a lone "---" would be rewritten into
                // a table CommonMark itself would never have rendered.
                cells(lines[index]).size != cells(lines[index + 1]).size) {
                index++
                continue
            }

            val start = index
            val table = mutableListOf<List<String>>()
            table += cells(lines[index])
            table += cells(lines[index + 1])
            index += 2
            while (index < lines.size && isPipeRow(lines[index]) && lines[index].isNotBlank()) {
                table += cells(lines[index])
                index++
            }

            // Column count from the widest row so no pasted cell is ever dropped from the
            // stored text (the renderer truncates extra cells visually; the formatter must
            // not edit content).
            val columnCount = table.maxOfOrNull { it.size } ?: 0
            if (columnCount == 0) continue
            val normalized = table.map { row ->
                (0 until columnCount).map { col -> row.getOrElse(col) { "" }.trim() }
            }
            // The separator row occupies the same visual width as its column. Include
            // alignment colons in the width calculation; otherwise right/center-aligned
            // separators can become wider than their data columns.
            val widths = (0 until columnCount).map { col ->
                val contentWidth = normalized.indices.filter { it != 1 }
                    .maxOfOrNull { normalized[it][col].length } ?: 0
                val separator = normalized.getOrNull(1)?.getOrNull(col).orEmpty()
                val markerWidth = (if (separator.startsWith(":")) 1 else 0) +
                    (if (separator.endsWith(":")) 1 else 0)
                maxOf(contentWidth, 3 + markerWidth)
            }
            for (rowIndex in normalized.indices) {
                val row = normalized[rowIndex]
                lines[start + rowIndex] = if (rowIndex == 1) {
                    "| " + row.mapIndexed { col, cell ->
                        val width = widths[col]
                        val left = cell.startsWith(":")
                        val right = cell.endsWith(":")
                        val dashCount = width - (if (left) 1 else 0) - (if (right) 1 else 0)
                        "-".repeat(dashCount).let { dashes ->
                            when {
                                left && right -> ":" + dashes + ":"
                                left -> ":" + dashes
                                right -> dashes + ":"
                                else -> dashes
                            }
                        }
                    }.joinToString(" | ") + " |"
                } else {
                    "| " + row.mapIndexed { col, cell -> cell.padEnd(widths[col]) }.joinToString(" | ") + " |"
                }
            }
        }
        return lines.joinToString("\n")
    }

    /**
     * True when [text] contains at least one valid Markdown table outside fenced code.
     * Used to switch a plain-text note to Markdown when pasted content carries a table,
     * even when the table is already perfectly aligned (nothing to reformat).
     */
    fun containsTable(text: String): Boolean {
        if (!text.contains('|')) return false
        val lines = text.split("\n")
        var inFence = false
        var fenceMarker = ""
        for (i in 0 until lines.size - 1) {
            val trimmed = lines[i].trimStart()
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                val marker = trimmed.take(3)
                inFence = if (!inFence) {
                    fenceMarker = marker
                    true
                } else {
                    if (marker == fenceMarker) {
                        fenceMarker = ""
                        false
                    } else {
                        true
                    }
                }
                continue
            }
            if (!inFence && isPipeRow(lines[i]) && isSeparatorRow(lines[i + 1]) &&
                cells(lines[i]).size == cells(lines[i + 1]).size) return true
        }
        return false
    }

    fun formatPasted(text: String, cursor: Int): Pair<String, Int> {
        val formatted = format(text)
        if (formatted == text) return text to cursor.coerceIn(0, text.length)
        val safeCursor = cursor.coerceIn(0, text.length)
        if (safeCursor == text.length) return formatted to formatted.length
        val lineIndex = text.substring(0, safeCursor).count { it == '\n' }
        val previousNewline = text.lastIndexOf('\n', (safeCursor - 1).coerceAtLeast(0))
        val lineStart = if (previousNewline < 0) 0 else previousNewline + 1
        val column = safeCursor - lineStart
        val atLineEnd = safeCursor == text.indexOf('\n', lineStart).let { if (it < 0) text.length else it }
        val formattedLines = formatted.split("\n")
        val before = formattedLines.take(lineIndex).sumOf { it.length + 1 }
        val current = formattedLines.getOrElse(lineIndex) { "" }
        val mappedColumn = if (atLineEnd) current.length else column.coerceAtMost(current.length)
        return formatted to (before + mappedColumn).coerceIn(0, formatted.length)
    }

    /**
     * True when [line] holds at least one unescaped pipe and some content. GFM tables do
     * not require outer pipes, so a single `A | B` row is a potential table row.
     */
    private fun isPipeRow(line: String): Boolean {
        if (line.trim().isEmpty()) return false
        var pipes = 0
        var i = 0
        while (i < line.length) {
            if (line[i] == '\\') {
                i += 2
                continue
            }
            if (line[i] == '|') pipes++
            i++
        }
        return pipes >= 1
    }

    /**
     * Splits a row into trimmed cells on unescaped pipes. `\|` is a literal pipe inside a
     * cell (GFM), so it never splits and is preserved verbatim; a trailing unescaped pipe
     * is an outer delimiter, a trailing `\|` is cell content.
     */
    private fun cells(line: String): List<String> {
        var value = line.trim()
        if (value.startsWith("|")) value = value.drop(1)
        if (value.endsWith("|") && !value.endsWith("\\|")) value = value.dropLast(1)
        val out = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < value.length) {
            when {
                value[i] == '\\' && i + 1 < value.length && value[i + 1] == '|' -> {
                    current.append("\\|")
                    i += 2
                }
                value[i] == '|' -> {
                    out += current.toString().trim()
                    current.setLength(0)
                    i++
                }
                else -> {
                    current.append(value[i])
                    i++
                }
            }
        }
        out += current.toString().trim()
        return out
    }

    /**
     * GFM delimiter cells: only dashes with optional leading/trailing colons. One dash is
     * enough (`:-:`), matching what CommonMark actually accepts — the old `-{3,}` rule
     * silently skipped perfectly valid pasted tables.
     */
    private fun isSeparatorRow(line: String): Boolean {
        val parts = cells(line)
        return parts.isNotEmpty() && parts.all { it.matches(Regex(":?-{1,}:?")) }
    }
}
