package com.nestnote.app

/**
 * Parses the title and body used when importing a Markdown document as a note.
 *
 * Keep this independent of Android URI access so the import rules can be regression-tested on the JVM.
 */
internal object MarkdownNoteParser {
    data class ParsedNote(val title: String, val body: String)

    fun parse(markdown: String, fallbackTitle: String): ParsedNote {
        val firstHeading = Regex("(?m)^#[ \\t]+([^\\r\\n]*?)[ \\t]*$").find(markdown)
        val title = firstHeading?.groupValues?.get(1)?.trim().orEmpty()
            .ifBlank { fallbackTitle.ifBlank { "Imported note" } }
        val body = if (firstHeading != null) markdown.removeRange(firstHeading.range).trim()
            else markdown.trim()
        return ParsedNote(title, body)
    }
}
