package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MermaidBlocksTest {

    @Test
    fun bodyWithoutFencesStaysOneMarkdownBlock() {
        val body = "# Title\n\nSome **text**\nmore"
        val blocks = MermaidBlocks.split(body)
        assertEquals(1, blocks.size)
        assertFalse(blocks[0].isMermaid)
        assertEquals(body, blocks[0].text)
        assertFalse(MermaidBlocks.hasMermaid(body))
    }

    @Test
    fun mermaidFenceIsExtractedWithoutFenceLines() {
        val body = "before\n\n```mermaid\nflowchart TD\n A --> B\n```\n\nafter"
        val blocks = MermaidBlocks.split(body)
        assertEquals(3, blocks.size)
        assertFalse(blocks[0].isMermaid)
        assertEquals("before\n\n", blocks[0].text)
        assertTrue(blocks[1].isMermaid)
        assertEquals("flowchart TD\n A --> B\n", blocks[1].text)
        assertFalse(blocks[2].isMermaid)
        assertEquals("\nafter\n", blocks[2].text)
        assertTrue(MermaidBlocks.hasMermaid(body))
    }

    @Test
    fun ordinaryFencedCodeIsNeverExtractedAsDiagram() {
        val body = "```text\nflowchart TD\n A --> B\n```"
        val blocks = MermaidBlocks.split(body)
        assertEquals(1, blocks.size)
        assertFalse(blocks[0].isMermaid)
        assertFalse(MermaidBlocks.hasMermaid(body))
    }

    @Test
    fun mermaidInfoStringInsideOrdinaryCodeFenceDoesNotSplit() {
        val body = "````\n```mermaid\nflowchart TD\n A --> B\n```\n````\n\ntext after"
        val blocks = MermaidBlocks.split(body)
        // The whole 4-backtick fence is ordinary code (a closing fence needs at least the
        // opening length), so nothing inside it may be pulled out as a diagram.
        assertEquals(1, blocks.size)
        assertFalse(blocks[0].isMermaid)
    }

    @Test
    fun mermaidFenceInfoIsCaseInsensitiveAndWordExact() {
        assertTrue(MermaidBlocks.hasMermaid("```Mermaid\ngraph TD\nA-->B\n```"))
        assertFalse("mermaid-x is a different language, not a diagram",
            MermaidBlocks.hasMermaid("```mermaid-x\ntext\n```"))
    }

    @Test
    fun unterminatedMermaidFenceStillYieldsItsSource() {
        val blocks = MermaidBlocks.split("```mermaid\ngraph TD\nA-->B")
        assertEquals(1, blocks.size)
        assertTrue(blocks[0].isMermaid)
        assertEquals("graph TD\nA-->B\n", blocks[0].text)
    }

    @Test
    fun wrapSourceFencesBareMermaidFiles() {
        val wrapped = MermaidBlocks.wrapSource("flowchart TD\n A --> B")
        assertEquals("```mermaid\nflowchart TD\n A --> B\n```", wrapped)
        assertTrue(MermaidBlocks.hasMermaid(wrapped))
    }

    @Test
    fun wrapSourceKeepsAlreadyFencedContentUntouched() {
        val already = "```mermaid\ngraph TD\nA-->B\n```"
        assertEquals(already, MermaidBlocks.wrapSource(already))
    }

    @Test
    fun pageHtmlEscapesBackticksBackslashesDollarAndScriptTags() {
        val hostile = "a\\b`c` \${danger}</script><script>alert(1)</script>"
        val html = MermaidPage.buildHtml(hostile, dark = true)

        // The source must survive as one JS string literal: no raw </script>, no raw ${,
        // no unescaped backticks/backslashes that would break out of the template.
        assertFalse(html.contains("</script>alert"))
        assertTrue(html.contains("<\\/script>"))
        assertTrue(html.contains("\\\${danger}"))
        assertFalse(html.contains("alert(1)</script>"))
        assertTrue(html.contains("theme: 'dark'"))
    }

    @Test
    fun pageHtmlUsesNeutralThemeForLightSurfaces() {
        val html = MermaidPage.buildHtml("graph TD\nA-->B", dark = false)
        assertTrue(html.contains("theme: 'neutral'"))
        assertTrue(html.contains("securityLevel: 'strict'"))
        assertTrue(html.contains("mermaid.min.js"))
    }
}
