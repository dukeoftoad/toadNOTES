package com.nestnote.app

/**
 * Splits note text into ordinary Markdown blocks and fenced mermaid diagram blocks so the
 * preview can render diagrams with the bundled offline mermaid.js runtime (see
 * `MermaidDiagramView`) while everything else keeps flowing through Markwon.
 *
 * The splitting rules mirror CommonMark fenced code blocks: 3+ backticks or tildes with up
 * to three leading spaces open a fence, a closing fence needs the same character, at least
 * the same length, and nothing but whitespace after it. A fence opens a diagram only when
 * its info string is exactly `mermaid` (case-insensitive); ordinary fenced code stays inside
 * the Markdown flow untouched, and a fence that opens inside an ordinary code block is never
 * mistaken for a diagram.
 *
 * Pure JVM on purpose: the splitting and wrapping rules are regression-tested without an
 * emulator, and nothing here touches Android classes.
 */
internal object MermaidBlocks {

    /** One preview block: ordinary Markdown, or the source of a mermaid diagram. */
    data class Block(val text: String, val isMermaid: Boolean)

    private val fenceOpen = Regex("""^ {0,3}(`{3,}|~{3,})[ \t]*(.*)$""")
    private val fenceClose = Regex("""^ {0,3}(`{3,}|~{3,})\s*$""")

    private fun infoIsMermaid(info: String): Boolean =
        info.trim().substringBefore(' ').substringBefore('\t').equals("mermaid", ignoreCase = true)

    /** True when [body] contains at least one ```mermaid fence (outside ordinary code fences). */
    fun hasMermaid(body: String): Boolean = split(body).any { it.isMermaid }

    /**
     * Splits [body] into blocks in document order. Mermaid blocks carry the diagram source
     * without the fence lines; all other text (including ordinary fenced code) is preserved
     * byte-for-byte so Markwon renders it exactly as it did before mermaid support existed.
     */
    fun split(body: String): List<Block> {
        if (!body.contains("```") && !body.contains("~~~")) return listOf(Block(body, false))
        if (!body.contains("mermaid", ignoreCase = true)) return listOf(Block(body, false))

        val blocks = mutableListOf<Block>()
        val text = StringBuilder(body.length)
        val diagram = StringBuilder()
        // State of the fence we are currently inside; null when not inside any fence.
        var insideMermaid = false
        var fenceChar: Char? = null
        var fenceLength = 0

        fun flushText() {
            if (text.isNotEmpty()) {
                blocks += Block(text.toString(), false)
                text.setLength(0)
            }
        }

        fun flushDiagram() {
            if (diagram.isNotEmpty()) {
                blocks += Block(diagram.toString(), true)
                diagram.setLength(0)
            }
        }

        for (line in body.lineSequence()) {
            if (insideMermaid) {
                val closing = fenceClose.matchEntire(line)
                if (closing != null && closing.groupValues[1][0] == fenceChar &&
                    closing.groupValues[1].length >= fenceLength
                ) {
                    // Leaving the diagram: the closing fence stays in the editor's source of
                    // truth (we never rewrite stored bodies), it just is not rendered.
                    insideMermaid = false
                    fenceChar = null
                    fenceLength = 0
                    flushDiagram()
                } else {
                    diagram.append(line).append('\n')
                }
                continue
            }
            if (fenceChar != null) {
                // Inside an ordinary code fence: everything is plain Markdown text.
                val closing = fenceClose.matchEntire(line)
                if (closing != null && closing.groupValues[1][0] == fenceChar &&
                    closing.groupValues[1].length >= fenceLength
                ) {
                    fenceChar = null
                    fenceLength = 0
                }
                text.append(line).append('\n')
                continue
            }
            val opening = fenceOpen.matchEntire(line)
            val marker = opening?.groupValues?.get(1).orEmpty()
            val info = opening?.groupValues?.get(2).orEmpty()
            val opensFence = opening != null &&
                (marker[0] != '`' || !info.contains('`'))
            if (opensFence && infoIsMermaid(info)) {
                flushText()
                insideMermaid = true
                fenceChar = marker[0]
                fenceLength = marker.length
            } else if (opensFence) {
                text.append(line).append('\n')
                fenceChar = marker[0]
                fenceLength = marker.length
            } else {
                text.append(line).append('\n')
            }
        }
        // Unterminated fences: keep whatever remains in its current bucket so nothing is lost.
        flushText()
        flushDiagram()
        return blocks
    }

    /**
     * Wraps a bare mermaid source (for example the content of an imported `.mmd` file) into a
     * ```mermaid fence so the ordinary preview pipeline renders it as a diagram. The source
     * is trimmed of surrounding blank lines only; its inner content is never rewritten.
     */
    fun wrapSource(source: String): String {
        val trimmed = source.trim('\n', '\r')
        if (trimmed.startsWith("```")) return trimmed
        return buildString {
            append("```mermaid\n")
            append(trimmed)
            if (!trimmed.endsWith('\n')) append('\n')
            append("```")
        }
    }
}
