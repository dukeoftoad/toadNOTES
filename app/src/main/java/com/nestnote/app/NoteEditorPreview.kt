package com.nestnote.app

import android.text.method.LinkMovementMethod
import android.widget.TextView
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
    renderMarkdown: Boolean = true
) {
    val context = LocalContext.current
    val segments = remember(body) { NoteText.voiceSegments(body) }
    val bodySp = textSize.bodySp.toFloat()
    Column(modifier.verticalScroll(rememberScrollState())) {
        segments.forEachIndexed { segmentIndex, segment ->
            if (segment.attachmentId == null) {
                if (segment.text.isNotEmpty()) {
                    val spanned = remember(segment.text, bodySp, ink, renderMarkdown) {
                        if (renderMarkdown) MarkdownPreview.toSpanned(context, segment.text, ink.toArgb())
                        else android.text.SpannableString(segment.text)
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .testTag("voice_preview_text_$segmentIndex"),
                        factory = { viewContext ->
                            TextView(viewContext).apply {
                                setTextColor(ink.toArgb())
                                // Markdown links must remain legible against the active note surface.
                                setLinkTextColor(ink.toArgb())
                                setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, bodySp)
                                setTextIsSelectable(true)
                                movementMethod = LinkMovementMethod.getInstance()
                                ViewCompat.setAccessibilityHeading(
                                    this,
                                    isStandaloneMarkdownHeading(segment.text, renderMarkdown)
                                )
                            }
                        },
                        update = { textView ->
                            textView.setTextColor(ink.toArgb())
                            textView.setLinkTextColor(ink.toArgb())
                            textView.textSize = bodySp
                            textView.text = spanned
                            ViewCompat.setAccessibilityHeading(
                                textView,
                                isStandaloneMarkdownHeading(segment.text, renderMarkdown)
                            )
                        }
                    )
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
