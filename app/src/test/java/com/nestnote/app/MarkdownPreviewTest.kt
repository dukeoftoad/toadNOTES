package com.nestnote.app

import android.graphics.Color
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.LineBackgroundSpan
import android.text.style.QuoteSpan
import io.noties.markwon.core.spans.EmphasisSpan
import io.noties.markwon.core.spans.StrongEmphasisSpan
import io.noties.markwon.ext.tables.TableRowSpan
import io.noties.markwon.ext.tables.TableSpan
import io.noties.markwon.ext.tasklist.TaskListSpan
import android.text.style.URLSpan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MarkdownPreviewTest {
    @Test
    fun rawHtmlIsRenderedAsTextRatherThanActiveHtml() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), """<script>alert("x")</script><img src="https://example.invalid/x.png">""")
        assertTrue(result.toString().contains("<script>"))
        assertFalse(result.getSpans(0, result.length, ImageSpan::class.java).isNotEmpty())
        assertFalse(result.getSpans(0, result.length, URLSpan::class.java).isNotEmpty())
    }

    @Test
    fun markdownLinksRemainClickableAndPlainHtmlDoesNotCreateLinks() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "[safe link](https://example.com) <a href=\"https://evil.invalid\">raw html</a>")
        val links = result.getSpans(0, result.length, URLSpan::class.java)
        assertTrue(links.any { it.url == "https://example.com" })
        assertFalse(links.any { it.url == "https://evil.invalid" })
    }

    @Test
    fun blockquotesWithInlineFormattingReceiveQuoteStyling() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "> **important**\n> *quoted text*")
        assertTrue(result.toString().contains("important"))
        assertTrue(result.toString().contains("quoted text"))
        assertFalse(result.toString().contains(">"))
    }

    @Test
    fun unsafeMarkdownLinkSchemesAreNotActionable() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), 
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
        assertTrue(MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "").isEmpty())
        assertTrue(MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "ordinary note").toString().contains("ordinary note"))
    }

    @Test
    fun explicitBlackMarkdownSpansUseThemeForegroundInDarkMode() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "# Dark heading\n\n- dark list item", Color.WHITE)
        assertTrue(result.toString().contains("Dark heading"))
        assertTrue(result.toString().contains("dark list item"))
        // Markwon styles headings structurally; it does not hardcode the parser's old black spans.
    }

    @Test
    fun horizontalRulesRenderAsThematicBreaks() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "before\n---\nafter\n\n___\nend", Color.WHITE)

        assertFalse(result.toString().contains("---"))
        assertFalse(result.toString().contains("___"))
    }

    @Test
    fun horizontalRuleLikeTextInsideFencedCodeIsNotConverted() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "```text\n---\n```")

        assertTrue(result.toString().contains("---"))
    }

    @Test
    fun differentFenceDelimiterDoesNotCloseAnOpenCodeFence() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "```text\n~~~\n> literal quote\n---\n```")

        assertTrue(result.toString().contains("~~~"))
        assertTrue(result.toString().contains("> literal quote"))
        assertTrue(result.toString().contains("---"))
    }

    @Test
    fun blockquoteMarkerIsParsedAsMarkdownRatherThanDisplayedLiterally() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "> A quoted thought")
        assertTrue(result.toString().contains("A quoted thought"))
        assertFalse(result.toString().contains(">"))
    }

    @Test
    fun markdownFormattingIsPreservedInTextSegmentsAroundVoiceNotes() {
        val id = "11111111-1111-4111-8111-111111111111"
        val body = "**before**\n[[voice:$id]]\n*after*"
        val segments = NoteText.voiceSegments(body)

        assertEquals(3, segments.size)
        assertTrue(MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), segments[0].text).toString().contains("before"))
        assertEquals(id, segments[1].attachmentId)
        assertTrue(MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), segments[2].text).toString().contains("after"))
    }

    @Test
    fun blockquoteIndicatorUsesActiveThemeForeground() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "> readable quote", Color.WHITE)
        assertTrue(result.toString().contains("readable quote"))
        assertFalse(result.toString().contains(">"))
    }

    @Test
    fun headingsBoldItalicListsAndLinksRetainFormattingAndReadableColors() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), 
            "# Heading\n\n**bold** and *italic*\n\n- list item\n\n[safe link](https://example.com)",
            Color.WHITE
        )
        val boldSpans = result.getSpans(0, result.length, StrongEmphasisSpan::class.java)
        val italicSpans = result.getSpans(0, result.length, EmphasisSpan::class.java)
        val links = result.getSpans(0, result.length, URLSpan::class.java)
        val foregrounds = result.getSpans(0, result.length, ForegroundColorSpan::class.java)

        assertTrue("heading text should be rendered", result.toString().contains("Heading"))
        assertTrue("bold style should be preserved", boldSpans.isNotEmpty())
        assertTrue("italic style should be preserved", italicSpans.isNotEmpty())
        assertTrue("list content should be preserved", result.toString().contains("list item"))
        assertTrue("links should remain clickable", links.any { it.url == "https://example.com" })
        assertTrue("parser foregrounds should use the theme color",
            foregrounds.all { it.foregroundColor == Color.WHITE })
    }
    @Test
    fun broaderBlockSyntaxKeepsHeadingsListsAndChecklistTextReadable() {
        val markdown = "# Heading\n\n- bullet\n- [ ] unchecked\n- [x] checked\n\n1. first\n2. second"
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), markdown).toString()

        assertTrue(result.contains("Heading"))
        assertTrue(result.contains("bullet"))
        assertTrue(result.contains("unchecked"))
        assertTrue(result.contains("checked"))
        assertTrue(result.contains("1. first") || result.contains("first"))
        assertTrue(result.contains("2. second") || result.contains("second"))
    }

    @Test
    fun fencedCodeKeepsMarkdownLookingContentReadableWithoutBlockFormatting() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), 
            "```kotlin\nval literal = \"**not bold**\"\n> not a quote\n---\n```"
        )
        val quotes = result.getSpans(0, result.length, QuoteSpan::class.java)
        val rules = result.getSpans(0, result.length, LineBackgroundSpan::class.java)

        assertTrue(result.toString().contains("not bold"))
        assertTrue(result.toString().contains("> not a quote"))
        assertTrue(result.toString().contains("---"))
        assertTrue("code samples must not be interpreted as blockquotes", quotes.isEmpty())
        assertTrue("code samples must not be interpreted as thematic breaks", rules.isEmpty())
    }

    @Test
    fun unsupportedMarkdownConstructsRemainReadableInsteadOfLosingContent() {
        val markdown = "A footnote[^1] and ~~struck~~ text\n\n[^1]: footnote body"
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), markdown).toString()

        assertTrue(result.contains("A footnote"))
        assertTrue(result.contains("footnote body"))
        assertTrue(result.contains("struck"))
    }

    @Test
    fun malformedEmphasisMarkersKeepReadableBodyTextInPreview() {
        val body = "*********jklloikkk**uuiii_iiio9_**"
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), body)

        assertTrue("first body token must not be lost", result.toString().contains("jklloikkk"))
        assertTrue("second body token must not be lost", result.toString().contains("uuiii_iiio9"))
    }

    @Test
    fun markdownPipeTablesUseMarkwonsNativeTableSpans() {
        val markdown = "| Name | Count |\n| --- | ---: |\n| Apples | 3 |"
        val rendered = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), markdown)

        // Markwon intentionally stores cell content inside ReplacementSpans so the table
        // extension can measure and draw aligned columns in a TextView. The cell text is
        // therefore not required to appear in Spanned.toString(); assert the rendering
        // contract rather than replacing it with a lossy plain-text table preprocessor.
        assertEquals("header and body should each be a native table row", 2,
            rendered.getSpans(0, rendered.length, TableRowSpan::class.java).size)
        assertEquals("the table block should have Markwon's table span", 1,
            rendered.getSpans(0, rendered.length, TableSpan::class.java).size)
        assertFalse("pipe delimiters should not be shown literally", rendered.toString().contains("|"))
    }

    @Test
    fun editorGlyphChecklistsAndBulletsRenderAsNativeListBlocks() {
        // Exact body from the device bug report (note "w22"): the editor's own list glyphs
        // used to render as merged paragraph text with the restarted list renumbered 1.→4.
        val body = "• Ikkmmihjkkkkkihbmlp\n• gjk\n1. K\n2. lokh\n3. Xff\n☑ EF\n" +
            "• **Efgyidesfg**\n1. *Sdfhfghjkyfg*\n2. Efgedg~~~~"
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), body)

        val tasks = result.getSpans(0, result.length, TaskListSpan::class.java)
        assertTrue("checkbox glyph lines must render as checked task list items",
            tasks.any { it.isChecked })
        val lines = result.toString().split('\n')
        assertTrue("restarted numbered list content must stay visible",
            lines.any { it.contains("Sdfhfghjkyfg") })
        assertTrue("final numbered list content must stay visible", lines.any { it.contains("Efgedg") })
        assertFalse("the two bullet lines must not render merged onto one line (the reported bug)",
            lines.any { it.contains("Ikkmmihjkkkkkihbmlp") && it.contains("gjk") })
        assertTrue("both bullet lines must stay visible",
            lines.any { it.contains("Ikkmmihjkkkkkihbmlp") } && lines.any { it.contains("gjk") })
        assertFalse("editor glyphs must not be shown literally",
            result.toString().contains("•") || result.toString().contains("☑"))
    }

    @Test
    fun singleNewlinesStayVisibleLineBreaksInsteadOfCollapsingIntoSpaces() {
        val result = MarkdownPreview.toSpanned(RuntimeEnvironment.getApplication(), "first line\nsecond line")
        assertTrue("each editor line must remain a separate visible line",
            result.toString().contains("first line\nsecond line"))
    }
}
