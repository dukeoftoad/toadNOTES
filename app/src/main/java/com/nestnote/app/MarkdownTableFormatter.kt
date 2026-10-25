package com.nestnote.app

/**
 * Aligns Markdown pipe tables after a multi-character paste.
 *
 * Only blocks with a valid Markdown separator row are touched. Fenced code and ordinary
 * pipe-containing prose are left unchanged. This formatter changes whitespace only; it
 * never changes cell contents or the stored table's Markdown semantics.
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
                !isSeparatorRow(lines[index + 1])) {
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

    private fun isPipeRow(line: String): Boolean =
        line.contains('|') && line.count { it == '|' } >= 2 && line.trim().isNotEmpty()

    private fun cells(line: String): List<String> {
        var value = line.trim()
        if (value.startsWith("|")) value = value.drop(1)
        if (value.endsWith("|")) value = value.dropLast(1)
        return value.split('|').map(String::trim)
    }

    private fun isSeparatorRow(line: String): Boolean {
        val parts = cells(line)
        return parts.isNotEmpty() && parts.all { it.matches(Regex(":?-{3,}:?")) }
    }
}
