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

    @Test
    fun shortGfmSeparatorsAreValidAndAligned() {
        // GFM accepts a single dash with colons; the old :-{3,} rule silently skipped it.
        val input = "| Name | Qty |\n|:-|-:|\n| Apple | 2 |"
        val formatted = MarkdownTableFormatter.format(input)
        assertEquals(
            "| Name  | Qty  |\n| :---- | ---: |\n| Apple | 2    |",
            formatted
        )
    }

    @Test
    fun rowsWithoutOuterPipesAreRecognized() {
        val input = "Name | Qty\n--- | ---\nApple | 2\nLonger | 12"
        val formatted = MarkdownTableFormatter.format(input)
        assertEquals(
            "| Name   | Qty |\n| ------ | --- |\n| Apple  | 2   |\n| Longer | 12  |",
            formatted
        )
    }

    @Test
    fun escapedPipesStayInsideCellsAndAreNeverSplit() {
        val input = "| A | B |\n|---|---|\n| x \\| y | z |"
        val formatted = MarkdownTableFormatter.format(input)
        // The escaped pipe must survive byte-for-byte inside its cell.
        assertTrue(formatted.contains("x \\| y"))
        assertEquals(
            "| A      | B   |\n| ------ | --- |\n| x \\| y | z   |",
            formatted
        )
    }

    @Test
    fun trailingEscapedPipeIsContentNotDelimiter() {
        val input = "| A |\n|---|\n| end\\| |"
        val formatted = MarkdownTableFormatter.format(input)
        // Row 1 is "| end\| |": the trailing pipe is an outer delimiter, the \| is content.
        assertTrue(formatted.contains("end\\|"))
    }

    @Test
    fun delimiterCellCountMismatchIsNeverRewritten() {
        // GFM: header and delimiter must have the same cell count; prose above a lone
        // "---" must stay exactly as written (CommonMark would see a setext heading).
        val input = "A | B\n---"
        assertEquals(input, MarkdownTableFormatter.format(input))
        assertFalse(MarkdownTableFormatter.containsTable(input))
    }

    @Test
    fun containsTableDetectsValidTablesIncludingWithoutOuterPipes() {
        assertTrue(MarkdownTableFormatter.containsTable("| A | B |\n|---|---|\n| 1 | 2 |"))
        assertTrue(MarkdownTableFormatter.containsTable("A | B\n- | -\n1 | 2"))
        // Inside a fenced code block it is not a table.
        assertFalse(
            MarkdownTableFormatter.containsTable("```text\n| A | B |\n|---|---|\n```")
        )
        // Prose with a pipe but no separator row is not a table.
        assertFalse(MarkdownTableFormatter.containsTable("yes | no\nmaybe | never"))
        // Short text below the paste gate is irrelevant to containsTable itself.
        assertTrue(MarkdownTableFormatter.containsTable("intro\n\nA | B\n- | -"))
    }

    @Test
    fun raggedRowsPadToHeaderColumnCountLikeGfmRenders() {
        val input = "| A | B |\n|---|---|\n| 1 | 2 | 3 |\n| 4 |"
        val formatted = MarkdownTableFormatter.format(input)
        val lines = formatted.lines()
        assertTrue("extra cell kept in its own padded column", lines[2].contains("| 3"))
        assertTrue("missing cell padded empty", lines[3].endsWith("|     |"))
    }
}
