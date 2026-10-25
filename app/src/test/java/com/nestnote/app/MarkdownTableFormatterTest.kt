package com.nestnote.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTableFormatterTest {
    @Test
    fun alignsPastedTableColumnsAndPreservesRightAlignment() {
        val input = "| Name | Qty |\n|---|---:|\n| Apple | 2 |\n| Very long | 12 |"
        val formatted = MarkdownTableFormatter.format(input)

        assertEquals(
            "| Name      | Qty  |\n| --------- | ---: |\n| Apple     | 2    |\n| Very long | 12   |",
            formatted
        )
        assertTrue(formatted.contains("---:"))
    }

    @Test
    fun leavesPipeTextInsideFencedCodeUntouched() {
        val input = "Example:\n\n```text\n| A | B |\n|---|---|\n```"
        assertEquals(input, MarkdownTableFormatter.format(input))
    }

    @Test
    fun ignoresOrdinaryPipeTextAndMalformedTables() {
        val input = "A | B\nnot a separator | row"
        assertEquals(input, MarkdownTableFormatter.format(input))
    }

    @Test
    fun cursorRemainsWithinFormattedTextAfterPaste() {
        val input = "| A | B |\n|---|---|\n| x | y |"
        val (formatted, cursor) = MarkdownTableFormatter.formatPasted(input, input.length)

        assertTrue(formatted.length > input.length)
        assertEquals(formatted.length, cursor)
    }

    @Test
    fun preservesAlignmentMarkers() {
        val input = "| Left | Center | Right |\n|:---|:---:|---:|\n| a | b | c |"
        val formatted = MarkdownTableFormatter.format(input)

        assertTrue(formatted.lines()[1].contains(":"))
        assertTrue(formatted.lines()[1].contains(Regex(":--+:")))
        assertTrue(formatted.lines()[1].contains(Regex("--+:")))
    }
}
