package com.nestnote.app

/**
 * Bridges the note editor's list dialect to the CommonMark dialect the preview renderer
 * (Markwon / commonmark-java) understands.
 *
 * The editor stores list state as literal glyphs (NoteText.BULLET / CHECKED / UNCHECKED:
 * "• ", "☑ ", "☐ ") and the Markdown export path (NoteText.toMarkdown) converts them to
 * "- ", "- [x] ", "- [ ] " when producing .md files. The preview used to feed the raw body
 * to the renderer instead, so glyph lines rendered as plain paragraph text: consecutive
 * glyph lines merged onto a single line (CommonMark soft breaks render as spaces), checkbox
 * lines lost their checked state and were absorbed into the preceding numbered item as lazy
 * continuations, and a numbered list that restarted (e.g. "3." followed by "1.") was taken
 * as a continuation of the previous list and silently renumbered.
 *
 * This adapter mirrors the export conversion and adds two render-only fixes:
 *  - single newlines become hard line breaks, so every line visible in the editor stays a
 *    separate line in the preview;
 *  - a top-level numbered item that neither continues nor repeats the previous number opens
 *    a new list, so the note's own numbering is preserved.
 *
 * Fenced code blocks (``` or ~~~) are left byte-identical. The result only feeds the preview
 * renderer; the stored note body is never rewritten.
 */
internal object MarkdownCompat {

    // The editor's own list glyphs (NoteText.BULLET/CHECKED/UNCHECKED). One trailing space or
    // tab is consumed, the same normalization NoteText.toMarkdown applies for export.
    private val checkedGlyph = Regex("""^(\s*)☑[ \t]?""")
    private val uncheckedGlyph = Regex("""^(\s*)☐[ \t]?""")
    private val bulletGlyph = Regex("""^(\s*)•[ \t]?""")

    // Top-level ordered item marker. Restart detection deliberately tracks only unindented
    // items (the regex is anchored with no leading-whitespace allowance) so nested list items
    // never trigger it.
    private val topLevelOrderedItem = Regex("""^(\d{1,9})[.)](?:[ \t]+|$)""")

    private val fenceOpen = Regex("""^ {0,3}(`{3,}|~{3,}).*$""")
    private val fenceClose = Regex("""^ {0,3}(`{3,}|~{3,})\s*$""")

    private const val hardBreakMarker = "  "

    /**
     * Returns [source] adapted for preview rendering. Already-CommonMark notes pass through
     * nearly untouched (single newlines gain a hard break; everything else is preserved).
     */
    fun adapt(source: String): String {
        if (source.isEmpty()) return source
        val builder = StringBuilder(source.length + 16)
        var fenceChar: Char? = null
        var fenceLength = 0
        var previousOrderedNumber = -1
        var sawOrderedItem = false
        var previousLineBlank = true
        var lastSeparator = "\n"

        for ((text, separator) in splitLines(source)) {
            if (fenceChar != null) {
                val closing = fenceClose.find(text)
                if (closing != null && closing.groupValues[1][0] == fenceChar &&
                    closing.groupValues[1].length >= fenceLength
                ) {
                    fenceChar = null
                    fenceLength = 0
                }
                builder.append(text).append(separator)
                previousLineBlank = text.isBlank()
            } else {
                val opening = fenceOpen.find(text)
                val marker = opening?.groupValues?.get(1).orEmpty()
                // A backtick fence cannot contain a backtick in its info string (CommonMark);
                // such a line is paragraph text, not a fence opener.
                if (marker.isNotEmpty() &&
                    (marker[0] != '`' || !text.substringAfter(marker).contains('`'))
                ) {
                    fenceChar = marker[0]
                    fenceLength = marker.length
                    builder.append(text).append(separator)
                    previousLineBlank = false
                } else {
                    var content = convertGlyphs(text)
                    val ordered = topLevelOrderedItem.find(content)
                    if (ordered != null) {
                        val number = ordered.groupValues[1].toInt()
                        if (sawOrderedItem && !previousLineBlank &&
                            number != previousOrderedNumber + 1 &&
                            number != previousOrderedNumber
                        ) {
                            // CommonMark would continue the previous list and renumber this
                            // item. A blank line closes that list so the item keeps the
                            // number the note actually shows.
                            builder.append(lastSeparator)
                        }
                        previousOrderedNumber = number
                        sawOrderedItem = true
                    }
                    if (separator.isNotEmpty() && content.isNotBlank() &&
                        !endsWithHardBreak(content)
                    ) {
                        content += hardBreakMarker
                    }
                    builder.append(content).append(separator)
                    previousLineBlank = text.isBlank()
                }
            }
            if (separator.isNotEmpty()) lastSeparator = separator
        }
        return builder.toString()
    }

    /**
     * Splits [source] into (line, following separator) pairs, preserving CRLF, lone CR, and
     * LF separators exactly; the final pair carries an empty separator when [source] does
     * not end with a line break.
     */
    private fun splitLines(source: String): List<Pair<String, String>> {
        val lines = ArrayList<Pair<String, String>>()
        var start = 0
        var index = 0
        while (index < source.length) {
            when (source[index]) {
                '\n' -> {
                    lines += source.substring(start, index) to "\n"
                    index++
                    start = index
                }
                '\r' -> {
                    if (index + 1 < source.length && source[index + 1] == '\n') {
                        lines += source.substring(start, index) to "\r\n"
                        index += 2
                    } else {
                        lines += source.substring(start, index) to "\r"
                        index++
                    }
                    start = index
                }
                else -> index++
            }
        }
        lines += source.substring(start) to ""
        return lines
    }

    /** Converts the editor's list glyphs to the CommonMark syntax export uses. */
    private fun convertGlyphs(line: String): String = when {
        line.contains('☑') -> checkedGlyph.replaceFirst(line, "$1- [x] ")
        line.contains('☐') -> uncheckedGlyph.replaceFirst(line, "$1- [ ] ")
        line.contains('•') -> bulletGlyph.replaceFirst(line, "$1- ")
        else -> line
    }

    /** True when the line already ends with CommonMark hard-break syntax (2+ spaces or an unescaped backslash). */
    private fun endsWithHardBreak(line: String): Boolean {
        if (line.endsWith(hardBreakMarker)) return true
        var backslashes = 0
        var index = line.length - 1
        while (index >= 0 && line[index] == '\\') {
            backslashes++
            index--
        }
        return backslashes % 2 == 1
    }
}
