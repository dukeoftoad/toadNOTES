package com.nestnote.app

import org.commonmark.node.AbstractVisitor
import org.commonmark.node.BulletList
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Text
import org.commonmark.parser.Parser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression coverage for the device bug report (Redmi veux, 2026-10-27, note "w22"):
 * the editor's own list glyphs (NoteText.BULLET/CHECKED/UNCHECKED) rendered as merged
 * paragraph text, the checkbox line was absorbed into the preceding ordered item, and a
 * restarted numbered list was renumbered as a continuation of the previous one.
 */
class MarkdownCompatTest {

    /** Exact note body from the bug-report screenshots. */
    private val bugReportBody = listOf(
        "• Ikkmmihjkkkkkihbmlp",
        "• gjk",
        "1. K",
        "2. lokh",
        "3. Xff",
        "☑ EF",
        "• **Efgyidesfg**",
        "1. *Sdfhfghjkyfg*",
        "2. Efgedg~~~~"
    ).joinToString("\n")

    @Test
    fun bugReportBodyIsAdaptedLineByLineWithoutMerging() {
        val expected = listOf(
            "- Ikkmmihjkkkkkihbmlp  ",
            "- gjk  ",
            "1. K  ",
            "2. lokh  ",
            "3. Xff  ",
            "- [x] EF  ",
            "- **Efgyidesfg**  ",
            "",
            "1. *Sdfhfghjkyfg*  ",
            "2. Efgedg~~~~"
        ).joinToString("\n")
        assertEquals(expected, MarkdownCompat.adapt(bugReportBody))
    }

    @Test
    fun rawBodyMergesIntoOneRenumberedListWhileAdaptedBodyKeepsSeparateLists() {
        val parser = Parser.builder().build()

        val raw = parser.parse(bugReportBody)
        assertEquals(
            "the renderer merges everything after the first ordered item into one list (the 1.→4. renumber bug)",
            1,
            topBlocks(raw).count { it is OrderedList }
        )
        assertTrue(
            "the checkbox glyph ends up as literal text inside the merged list item",
            collectText(raw).contains("☑")
        )

        val adapted = parser.parse(MarkdownCompat.adapt(bugReportBody))
        assertEquals(
            "bullets, the numbered list, the task list, and the restarted numbering must be separate blocks",
            listOf(
                BulletList::class.java,
                OrderedList::class.java,
                BulletList::class.java,
                OrderedList::class.java
            ),
            topBlocks(adapted).map { it.javaClass }
        )
        val adaptedText = collectText(adapted)
        assertFalse("no editor glyph may leak into the rendered source", adaptedText.contains("☑"))
        assertFalse(adaptedText.contains("•"))
    }

    @Test
    fun singleNewlinesBecomeHardBreaksAndExistingBreakSyntaxIsUntouched() {
        val adapted = MarkdownCompat.adapt("first\nsecond  \nthird\\\nfourth\nfifth")
        assertEquals(
            listOf("first  ", "second  ", "third\\", "fourth  ", "fifth").joinToString("\n"),
            adapted
        )
    }

    @Test
    fun fencedCodeStaysByteIdenticalIncludingTildeRunsInsideBacktickFences() {
        val adapted = MarkdownCompat.adapt("```text\n• literal\n~~~\n---\n```\nafter\nmore")
        assertEquals(
            listOf("```text", "• literal", "~~~", "---", "```", "after  ", "more").joinToString("\n"),
            adapted
        )
    }

    @Test
    fun repeatedLazyOneMarkersStayInOneList() {
        val adapted = MarkdownCompat.adapt("1. a\n1. b\n1. c")
        assertEquals(listOf("1. a  ", "1. b  ", "1. c").joinToString("\n"), adapted)
    }

    @Test
    fun numberingRestartOrSkipStartsANewListKeepingTheUserNumbers() {
        val restarted = MarkdownCompat.adapt("1. a\n2. b\n3. c\n1. restarted")
        assertEquals(
            listOf("1. a  ", "2. b  ", "3. c  ", "", "1. restarted").joinToString("\n"),
            restarted
        )
        assertEquals("2. a  \n\n5. b", MarkdownCompat.adapt("2. a\n5. b"))
    }

    @Test
    fun editorGlyphsConvertWithIndentAndPreserveCrlfEndings() {
        assertEquals(
            "- bullet  \n  - nested  \n- [ ] todo  \n- [x] done",
            MarkdownCompat.adapt("• bullet\n  • nested\n☐ todo\n☑ done")
        )
        assertEquals("- a  \r\n- b  \r\n", MarkdownCompat.adapt("• a\r\n• b\r\n"))
    }

    @Test
    fun plainSingleLinesAndEmptyInputStayUnchanged() {
        assertEquals("just a note", MarkdownCompat.adapt("just a note"))
        assertEquals("", MarkdownCompat.adapt(""))
    }

    @Test
    fun blankLinesAreNotBrokenAndExistingSeparatorsAreNotDuplicated() {
        assertEquals("1. a  \n\n3. b", MarkdownCompat.adapt("1. a\n\n3. b"))
        assertEquals("para one  \n\npara two", MarkdownCompat.adapt("para one\n\npara two"))
    }

    private fun topBlocks(document: Node): List<Node> {
        val blocks = mutableListOf<Node>()
        var child = document.firstChild
        while (child != null) {
            blocks += child
            child = child.next
        }
        return blocks
    }

    private fun collectText(node: Node): String {
        val builder = StringBuilder()
        node.accept(object : AbstractVisitor() {
            override fun visit(text: Text) {
                builder.append(text.literal)
            }
        })
        return builder.toString()
    }
}
