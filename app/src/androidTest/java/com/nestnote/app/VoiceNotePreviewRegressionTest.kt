package com.nestnote.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI regression coverage for the actual inline voice-note preview.
 * A deliberately absent attachment exercises the safe fallback card without
 * depending on a real recording, database fixture, or device audio output.
 */
@RunWith(AndroidJUnit4::class)
class VoiceNotePreviewRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun previewKeepsTextAndMissingVoiceCardInDocumentOrder() {
        val attachmentId = "11111111-1111-4111-8111-111111111111"
        val body = "First paragraph\\n\\n[[voice:$attachmentId]]\\n\\nLast paragraph"

        composeRule.setContent {
            MaterialTheme {
                PreviewWithInlineVoice(
                    body = body,
                    noteId = Long.MIN_VALUE,
                    textSize = EditorTextSize.MEDIUM,
                    ink = Color.Black,
                    activePlayerId = null,
                    onPlayRequested = {},
                    onStopRequested = {},
                    onDeleteRequested = {}
                )
            }
        }
        composeRule.waitForIdle()

        val firstText = composeRule.onNodeWithTag("voice_preview_text_0")
        val player = composeRule.onNodeWithTag("voice_preview_player")
        val lastText = composeRule.onNodeWithTag("voice_preview_text_2")

        firstText.assertIsDisplayed()
        player.assertIsDisplayed()
        lastText.assertIsDisplayed()
        composeRule.onNodeWithText("Voice note unavailable").assertIsDisplayed()

        val firstTop = firstText.fetchSemanticsNode().boundsInRoot.top
        val playerTop = player.fetchSemanticsNode().boundsInRoot.top
        val lastTop = lastText.fetchSemanticsNode().boundsInRoot.top
        assertTrue("First paragraph must precede the voice card", firstTop < playerTop)
        assertTrue("Voice card must precede the last paragraph", playerTop < lastTop)
    }

    @Test
    fun adjacentVoiceMarkersRenderSeparateFallbackCards() {
        val first = "11111111-1111-4111-8111-111111111111"
        val second = "22222222-2222-4222-8222-222222222222"
        val body = "Before\\n[[voice:$first]][[voice:$second]]\\nAfter"

        composeRule.setContent {
            MaterialTheme {
                PreviewWithInlineVoice(
                    body = body,
                    noteId = Long.MIN_VALUE,
                    textSize = EditorTextSize.MEDIUM,
                    ink = Color.Black,
                    activePlayerId = null,
                    onPlayRequested = {},
                    onStopRequested = {},
                    onDeleteRequested = {}
                )
            }
        }
        composeRule.waitForIdle()

        // Both cards are real InlineVoicePlayer instances; both safely render
        // their missing-attachment fallback instead of crashing.
        composeRule.onAllNodesWithText("Voice note unavailable").assertCountEquals(2)
    }
}
