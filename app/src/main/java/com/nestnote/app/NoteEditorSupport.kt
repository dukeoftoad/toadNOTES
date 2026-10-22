package com.nestnote.app

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import kotlin.math.roundToInt

private val STANDALONE_HEADING = Regex("^#{1,6}\\s+\\S(?:.*\\S)?$")

internal fun collapsibleSlotHeight(measuredHeight: Int, fraction: Float): Int =
    (measuredHeight * fraction).roundToInt().coerceAtLeast(0)
/**
 * The preview exposes each voice-free segment as one selectable TextView (one
 * accessibility node). Mark it as a heading only when the entire segment is one
 * Markdown ATX heading. Mixed paragraphs or multiple headings remain a normal text
 * node; marking the whole node as a heading would misrepresent its content.
 * Rendering mixed Markdown as separate semantic block nodes is a separate change.
 */
internal fun isStandaloneMarkdownHeading(text: String, renderMarkdown: Boolean = true): Boolean =
    renderMarkdown && STANDALONE_HEADING.matches(text.trim())



/**
 * Hides internal attachment IDs while editing without changing the saved note body.
 * Each marker is displayed as a compact, styled voice-note chip with a visible
 * waveform motif and recording name. Offset mapping keeps text selection and
 * cursor movement anchored to the underlying marker.
 */
internal class VoiceMarkerVisualTransformation(
    private val chipBackground: Color,
    private val chipForeground: Color,
    private val names: Map<String, String> = emptyMap()
) : VisualTransformation {
    private companion object {
        val markerRegex = Regex("""\[\[voice:([^\]\n]+)\]\]""")
        val validAttachmentId =
            Regex("""(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}""")
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val matches = markerRegex.findAll(text.text).toList()
        if (matches.isEmpty()) return TransformedText(text, OffsetMapping.Identity)

        val output = AnnotatedString.Builder()
        val ranges = mutableListOf<MarkerOffset>()
        var sourceCursor = 0
        var outputCursor = 0

        matches.forEach { match ->
            val prefix = text.text.substring(sourceCursor, match.range.first)
            output.append(prefix)
            outputCursor += prefix.length
            val attachmentId = match.groupValues[1]
            val displayName = if (validAttachmentId.matches(attachmentId)) names[attachmentId] ?: "Voice note" else "Voice note unavailable"
            // TextField visual transformations cannot embed a live composable.
            // Use a compact static waveform motif here; the preview player animates it.
            val waveform = "▂▄▆█▆▄"
            val label = "  $waveform  " + displayName.take(28) + "  "
            val transformedStart = outputCursor
            output.pushStyle(
                SpanStyle(
                    color = chipForeground,
                    background = chipBackground,
                    fontWeight = FontWeight.SemiBold
                )
            )
            output.append(label)
            output.pop()
            outputCursor += label.length
            ranges += MarkerOffset(match.range.first, match.range.last + 1, transformedStart, outputCursor)
            sourceCursor = match.range.last + 1
        }
        output.append(text.text.substring(sourceCursor))

        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val safe = offset.coerceIn(0, text.length)
                var delta = 0
                for (range in ranges) {
                    if (safe < range.originalStart) break
                    if (safe <= range.originalEnd) {
                        return if (safe == range.originalEnd) range.transformedEnd
                        else range.transformedStart
                    }
                    delta += (range.transformedEnd - range.transformedStart) -
                        (range.originalEnd - range.originalStart)
                }
                return (safe + delta).coerceIn(0, output.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val safe = offset.coerceIn(0, output.length)
                var delta = 0
                for (range in ranges) {
                    if (safe < range.transformedStart) break
                    if (safe <= range.transformedEnd) {
                        return if (safe == range.transformedEnd) range.originalEnd
                        else range.originalStart
                    }
                    delta += (range.originalEnd - range.originalStart) -
                        (range.transformedEnd - range.transformedStart)
                }
                return (safe + delta).coerceIn(0, text.length)
            }
        }
        return TransformedText(output.toAnnotatedString(), mapping)
    }

    private data class MarkerOffset(
        val originalStart: Int,
        val originalEnd: Int,
        val transformedStart: Int,
        val transformedEnd: Int
    )
}
