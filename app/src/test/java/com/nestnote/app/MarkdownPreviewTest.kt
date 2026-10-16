package com.nestnote.app

import android.text.style.ImageSpan
import android.text.style.URLSpan
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
}
