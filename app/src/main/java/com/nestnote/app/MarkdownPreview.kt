package com.nestnote.app

import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.QuoteSpan
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
        // the parser can recognize blockquote prefixes. Escaping every '>' turned quote
        // prefixes into literal text before Markdown parsing ever began.
        val escaped = markdown
            .replace("&", "&amp;")
            .replace("<", "&lt;")
        val html = GsSimpleMarkdownParser()
            .parse(escaped, "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
            .html
        val parsed = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)

        // The lightweight upstream parser strips quote markers inconsistently and does
        // not expose blockquote styling. Apply a native QuoteSpan to the corresponding
        // rendered text ranges so quotes remain visually distinguishable.
        val result = SpannableStringBuilder(parsed)
        var searchFrom = 0
        markdown.lineSequence().forEach { line ->
            val quote = Regex("""^\s*>\s?(.*)$""").matchEntire(line)
            val quotedText = quote?.groupValues?.get(1).orEmpty()
            if (quote != null && quotedText.isNotEmpty()) {
                val start = result.toString().indexOf(quotedText, searchFrom)
                if (start >= 0) {
                    val end = start + quotedText.length
                    result.setSpan(
                        QuoteSpan(0xFF8A80C8.toInt()),
                        start,
                        end,
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    searchFrom = end
                }
            }
        }
        return result
    }
}
