package com.nestnote.app

import com.nestnote.app.opensource.GsSimpleMarkdownParser
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun rendersCommonMarkdownFormatting() {
        val html = GsSimpleMarkdownParser()
            .parse("# Heading\n\n**bold** and *italic* with `code`", "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
            .html

        assertTrue(html.contains("<b>"))
        assertTrue(html.contains("<i>"))
        assertTrue(html.contains("monospace"))
        assertTrue(html.contains("Heading"))
    }

    @Test
    fun rendersSimpleListsAndLinks() {
        val html = GsSimpleMarkdownParser()
            .parse("- first\n- second\n[docs](https://example.com)", "", GsSimpleMarkdownParser.FILTER_ANDROID_TEXTVIEW)
            .html

        assertTrue(html.contains("first"))
        assertTrue(html.contains("second"))
        assertTrue(html.contains("https://example.com"))
    }
}
