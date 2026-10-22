package com.nestnote.app

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.LineBackgroundSpan
import android.text.style.QuoteSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.graphics.Typeface
import android.text.style.URLSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MarkdownPreviewTest {
    @Test
    fun rawHtmlIsRenderedAsTextRatherThanActiveHtml() {
        val result = MarkdownPreview.toSpanned("""<script>alert("x")</script><img src="https://example.invalid/x.png">""")
        assertTrue(result.toString().contains("<script>"))
        assertFalse(result.getSpans(0, result.length, ImageSpan::class.java).isNotEmpty())
        assertFalse(result.getSpans(0, result.length, URLSpan::class.java).isNotEmpty())
    }

    @Test
    fun markdownLinksRemainClickableAndPlainHtmlDoesNotCreateLinks() {
        val result = MarkdownPreview.toSpanned("[safe link](https://example.com) <a href=\"https://evil.invalid\">raw html</a>")
        val links = result.getSpans(0, result.length, URLSpan::class.java)
        assertTrue(links.any { it.url == "https://example.com" })
        assertFalse(links.any { it.url == "https://evil.invalid" })
    }

    @Test
    fun blockquotesWithInlineFormattingReceiveQuoteStyling() {
        val result = MarkdownPreview.toSpanned("> **important**\n> *quoted text*")
        val quoteSpans = result.getSpans(0, result.length, QuoteSpan::class.java)

        assertTrue(result.toString().contains("important"))
        assertTrue(result.toString().contains("quoted text"))
        assertEquals(2, quoteSpans.size)
    }

    @Test
    fun unsafeMarkdownLinkSchemesAreNotActionable() {
        val result = MarkdownPreview.toSpanned(
            "[safe](https://example.com) [mail](mailto:notes@example.com) " +
                "[script](javascript:alert%281%29) [local](file:///etc/passwd) " +
                "[data](data:text/html,hello)"
        )
        val links = result.getSpans(0, result.length, URLSpan::class.java)
        assertTrue(links.any { it.url == "https://example.com" })
        assertTrue(links.any { it.url == "mailto:notes@example.com" })
        assertFalse(links.any { it.url.startsWith("javascript:", ignoreCase = true) })
        assertFalse(links.any { it.url.startsWith("file:", ignoreCase = true) })
        assertFalse(links.any { it.url.startsWith("data:", ignoreCase = true) })
    }

    @Test
    fun emptyAndPlainTextPreviewAreHandled() {
        assertTrue(MarkdownPreview.toSpanned("").isEmpty())
        assertTrue(MarkdownPreview.toSpanned("ordinary note").toString().contains("ordinary note"))
    }

    @Test
    fun explicitBlackMarkdownSpansUseThemeForegroundInDarkMode() {
        val result = MarkdownPreview.toSpanned("# Dark heading\n\n- dark list item", Color.WHITE)
        val spans = result.getSpans(0, result.length, ForegroundColorSpan::class.java)
        assertTrue(spans.isNotEmpty())
        assertTrue(spans.all { it.foregroundColor == Color.WHITE })
    }

    @Test
    fun horizontalRulesRenderAsThematicBreaks() {
        val result = MarkdownPreview.toSpanned("before\n---\nafter\n\n___\nend", Color.WHITE)

        assertFalse(result.toString().contains("---"))
        assertFalse(result.toString().contains("___"))
        assertTrue(result.getSpans(0, result.length, LineBackgroundSpan::class.java).isNotEmpty())
    }

    @Test
    fun horizontalRuleLikeTextInsideFencedCodeIsNotConverted() {
        val result = MarkdownPreview.toSpanned("```text\n---\n```")

        assertTrue(result.toString().contains("---"))
        assertTrue(result.getSpans(0, result.length, LineBackgroundSpan::class.java).isEmpty())
    }

    @Test
    fun differentFenceDelimiterDoesNotCloseAnOpenCodeFence() {
        val result = MarkdownPreview.toSpanned("```text\n~~~\n> literal quote\n---\n```")

        assertTrue(result.toString().contains("~~~"))
        assertTrue(result.toString().contains("> literal quote"))
        assertTrue(result.toString().contains("---"))
        assertTrue(result.getSpans(0, result.length, QuoteSpan::class.java).isEmpty())
        assertTrue(result.getSpans(0, result.length, LineBackgroundSpan::class.java).isEmpty())
    }

    @Test
    fun blockquoteMarkerIsParsedAsMarkdownRatherThanDisplayedLiterally() {
        val result = MarkdownPreview.toSpanned("> A quoted thought")
        assertTrue(result.toString().contains("A quoted thought"))
        assertFalse(result.toString().contains(">"))
        assertTrue(result.getSpans(0, result.length, QuoteSpan::class.java).isNotEmpty())
    }

    @Test
    fun markdownFormattingIsPreservedInTextSegmentsAroundVoiceNotes() {
        val id = "11111111-1111-4111-8111-111111111111"
        val body = "**before**\n[[voice:$id]]\n*after*"
        val segments = NoteText.voiceSegments(body)

        assertEquals(3, segments.size)
        assertTrue(MarkdownPreview.toSpanned(segments[0].text).toString().contains("before"))
        assertEquals(id, segments[1].attachmentId)
        assertTrue(MarkdownPreview.toSpanned(segments[2].text).toString().contains("after"))
    }

    @Test
    fun blockquoteIndicatorUsesActiveThemeForeground() {
        val result = MarkdownPreview.toSpanned("> readable quote", Color.WHITE)
        val quoteSpans = result.getSpans(0, result.length, QuoteSpan::class.java)

        assertTrue(quoteSpans.isNotEmpty())
        assertTrue(quoteSpans.all { it.color == Color.WHITE })
    }

    @Test
    fun headingsBoldItalicListsAndLinksRetainFormattingAndReadableColors() {
        val result = MarkdownPreview.toSpanned(
            "# Heading\n\n**bold** and *italic*\n\n- list item\n\n[safe link](https://example.com)",
            Color.WHITE
        )
        val sizes = result.getSpans(0, result.length, RelativeSizeSpan::class.java)
        val styles = result.getSpans(0, result.length, StyleSpan::class.java)
        val links = result.getSpans(0, result.length, URLSpan::class.java)
        val foregrounds = result.getSpans(0, result.length, ForegroundColorSpan::class.java)

        assertTrue("heading size should be preserved", sizes.isNotEmpty())
        assertTrue("bold style should be preserved", styles.any { it.style == Typeface.BOLD })
        assertTrue("italic style should be preserved", styles.any { it.style == Typeface.ITALIC })
        assertTrue("list content should be preserved", result.toString().contains("list item"))
        assertTrue("links should remain clickable", links.any { it.url == "https://example.com" })
        assertTrue("parser foregrounds should use the theme color",
            foregrounds.all { it.foregroundColor == Color.WHITE })
    }
}
