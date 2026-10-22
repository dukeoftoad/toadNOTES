package com.nestnote.app

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import java.util.Locale

/**
 * CommonMark-based native-text Markdown preview.
 *
 * Note bodies are untrusted. Raw HTML rendering is not enabled, and generated links are
 * restricted to supported schemes. Rendering never rewrites the stored Markdown source.
 */
internal object MarkdownPreview {
    private val allowedLinkSchemes = setOf("http", "https", "mailto")

    fun toSpanned(
        context: Context,
        markdown: String,
        foregroundColor: Int = Color.BLACK
    ): Spanned {
        val renderer = Markwon.builder(context.applicationContext)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context.applicationContext))
            .build()
        val rendered = SpannableStringBuilder(renderer.toMarkdown(markdown))

        if (foregroundColor != Color.BLACK) {
            // Normalize every explicit foreground span to the active note color. Markdown
            // renderers may use more than pure black for headings or other block elements;
            // leaving those defaults intact can make text unreadable on dark surfaces.
            rendered.getSpans(0, rendered.length, ForegroundColorSpan::class.java)
                .forEach { span ->
                    val start = rendered.getSpanStart(span)
                    val end = rendered.getSpanEnd(span)
                    val flags = rendered.getSpanFlags(span)
                    rendered.removeSpan(span)
                    if (start >= 0 && end >= start) {
                        rendered.setSpan(ForegroundColorSpan(foregroundColor), start, end, flags)
                    }
                }
        }

        // Never allow note content to launch arbitrary Android intents.
        rendered.getSpans(0, rendered.length, URLSpan::class.java)
            .filter { span ->
                val scheme = runCatching {
                    Uri.parse(span.url).scheme?.lowercase(Locale.ROOT)
                }.getOrNull()
                scheme !in allowedLinkSchemes
            }
            .forEach(rendered::removeSpan)

        return rendered
    }
}
