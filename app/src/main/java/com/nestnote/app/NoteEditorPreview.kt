package com.nestnote.app

import android.text.method.LinkMovementMethod
import android.widget.TextView
import android.graphics.Typeface
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat

@Composable
internal fun PreviewWithInlineVoice(
    body: String,
    noteId: Long,
    textSize: EditorTextSize,
    ink: Color,
    activePlayerId: String?,
    onPlayRequested: (String) -> Unit,
    onStopRequested: () -> Unit,
    onDeleteRequested: (String) -> Unit,
    modifier: Modifier = Modifier,
    renderMarkdown: Boolean = true,
    bodyTextSp: Int = textSize.bodySp,
    typeface: Typeface = Typeface.DEFAULT,
    dark: Boolean = false
) {
    val context = LocalContext.current
    val segments = remember(body) { NoteText.voiceSegments(body) }
    val bodySp = bodyTextSp.toFloat()
    Column(modifier.verticalScroll(rememberScrollState())) {
        segments.forEachIndexed { segmentIndex, segment ->
            if (segment.attachmentId == null) {
                if (segment.text.isNotEmpty()) {
                    val blocks = if (renderMarkdown) {
                        remember(segment.text) { MermaidBlocks.split(segment.text) }
                    } else {
                        remember(segment.text) {
                            listOf(MermaidBlocks.Block(segment.text, isMermaid = false))
                        }
                    }
                    blocks.forEachIndexed { blockIndex, block ->
                        if (block.isMermaid) {
                            MermaidDiagramView(
                                source = block.text,
                                dark = dark,
                                modifier = Modifier.fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .testTag("preview_mermaid_$segmentIndex")
                            )
                        } else if (block.text.isNotEmpty()) {
                            MarkdownTextBlock(
                                text = block.text,
                                bodySp = bodySp,
                                typeface = typeface,
                                ink = ink,
                                renderMarkdown = renderMarkdown,
                                // Keep the historical per-segment tag for segments without
                                // diagrams; split segments get a block-qualified suffix.
                                testTag = if (blocks.size == 1) "voice_preview_text_$segmentIndex"
                                else "voice_preview_text_${segmentIndex}_$blockIndex"
                            )
                        }
                    }
                }
            } else {
                InlineVoicePlayer(
                    noteId = noteId,
                    attachmentId = segment.attachmentId!!,
                    activePlayerId = activePlayerId,
                    onPlayRequested = onPlayRequested,
                    onStopRequested = onStopRequested,
                    onDeleteRequested = onDeleteRequested,
                    modifier = Modifier.padding(vertical = 6.dp)
                        .testTag("voice_preview_player")
                )
            }
        }
    }
}

/**
 * One rendered Markdown block inside the preview. Kept as its own composable so the
 * mermaid splitter can interleave diagrams between ordinary text blocks without
 * duplicating the TextView wiring.
 */
@Composable
private fun MarkdownTextBlock(
    text: String,
    bodySp: Float,
    typeface: Typeface,
    ink: Color,
    renderMarkdown: Boolean,
    testTag: String
) {
    val context = LocalContext.current
    val spanned = remember(text, bodySp, ink, renderMarkdown) {
        if (renderMarkdown) MarkdownPreview.toSpanned(context, text, ink.toArgb())
        else android.text.SpannableString(text)
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .testTag(testTag),
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextColor(ink.toArgb())
                // Markdown links must remain legible against the active note surface.
                setLinkTextColor(ink.toArgb())
                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, bodySp)
                this.typeface = typeface
                setTextIsSelectable(true)
                movementMethod = LinkMovementMethod.getInstance()
                ViewCompat.setAccessibilityHeading(
                    this,
                    isStandaloneMarkdownHeading(text, renderMarkdown)
                )
            }
        },
        update = { textView ->
            textView.setTextColor(ink.toArgb())
            textView.setLinkTextColor(ink.toArgb())
            textView.textSize = bodySp
            textView.typeface = typeface
            textView.text = spanned
            ViewCompat.setAccessibilityHeading(
                textView,
                isStandaloneMarkdownHeading(text, renderMarkdown)
            )
        }
    )
}
