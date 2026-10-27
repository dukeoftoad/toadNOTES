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
import io.noties.markwon.ext.tables.TablePlugin
import java.util.Locale

/**
 * CommonMark-based native-text Markdown preview.
 *
 * Note bodies are untrusted. Raw HTML rendering is not enabled, and generated links are
 * restricted to supported schemes. Rendering never rewrites the stored Markdown source.
 */
internal object MarkdownPreview {
    private val allowedLinkSchemes = setOf("http", "https", "mailto")

    @Volatile
    private var cachedRenderer: Markwon? = null

    /**
     * Markwon plugin registration is comparatively expensive. Reuse one application-scoped
     * renderer instead of rebuilding it for every preview update or AndroidView recomposition.
     */
    private fun renderer(context: Context): Markwon {
        cachedRenderer?.let { return it }
        return synchronized(this) {
            cachedRenderer ?: run {
                val appContext = context.applicationContext
                Markwon.builder(appContext)
                    .usePlugin(StrikethroughPlugin.create())
                    .usePlugin(TaskListPlugin.create(appContext))
                    .usePlugin(TablePlugin.create(appContext))
                    .build()
                    .also { cachedRenderer = it }
            }
        }
    }

    fun toSpanned(
        context: Context,
        markdown: String,
        foregroundColor: Int = Color.BLACK
    ): Spanned {
        // Keep table blocks in Markwon's native rendering pipeline. Table cell content is
        // held by drawing spans, so tests should assert the table spans rather than rely on
        // Spanned.toString() to expose cell text.
        //
        // The editor stores list state as literal glyphs (•, ☑, ☐) and relies on visible
        // line breaks; align the body with the CommonMark dialect the renderer expects (the
        // same conversion Markdown export performs) before handing it over.
        val rendered = SpannableStringBuilder(renderer(context).toMarkdown(MarkdownCompat.adapt(markdown)))

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
