package com.nestnote.app

import android.text.Spanned
import androidx.core.text.HtmlCompat
import com.nestnote.app.opensource.GsSimpleMarkdownParser

/**
 * Safe, intentionally small Markdown preview using Markor's CC0 parser.
 * Raw HTML is escaped before parsing because note bodies are user-controlled text.
 * The upstream parser supports common inline formatting and simple headings/lists only.
 */
internal object MarkdownPreview {
    fun toSpanned(markdown: String): Spanned {
        // Escape HTML-opening characters, but leave Markdown's '>' syntax intact so
        // the parser can recognize blockquotes. Escaping every '>' turned quote
        // prefixes into literal text before Markdown parsing ever began.
        val escaped = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
        val html = GsSimpleMarkdownParser()
            .parse(escaped, "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
            .html
        return HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
    }
}
