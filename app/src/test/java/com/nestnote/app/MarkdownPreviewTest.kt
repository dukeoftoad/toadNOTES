package com.nestnote.app

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.QuoteSpan
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
    fun blockquoteMarkerIsParsedAsMarkdownRatherThanDisplayedLiterally() {
        val result = MarkdownPreview.toSpanned("> A quoted thought")
        assertTrue(result.toString().contains("A quoted thought"))
        assertFalse(result.toString().contains(">"))
        assertTrue(result.getSpans(0, result.length, QuoteSpan::class.java).isNotEmpty())
    }

    @Test
    fun markdownFormattingIsPreservedInTextSegmentsAroundVoiceNotes() {
        val id = "11111111-1111-4111-8111-111111111111"
        val body = "**before**\\n[[voice:$id]]\\n*after*"
        val segments = NoteText.voiceSegments(body)

        assertEquals(3, segments.size)
        assertTrue(MarkdownPreview.toSpanned(segments[0].text).toString().contains("before"))
        assertEquals(id, segments[1].attachmentId)
        assertTrue(MarkdownPreview.toSpanned(segments[2].text).toString().contains("after"))
    }
}
