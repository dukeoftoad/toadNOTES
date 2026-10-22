package com.nestnote.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for SlimRailPanel's first layout and state transitions.
 * Gesture interruption, predictive-back cancellation, and editor integration
 * still require device/integration coverage beyond this focused component test.
 */
@RunWith(AndroidJUnit4::class)
class SlimRailPanelRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun initialCompositionStartsClosedWithoutReadingUninitializedOffset() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    val state = rememberSlimRailState()
                    SlimRailPanel(
                        state = state,
                        contentDescription = "Test panel",
                        panelContent = { Text("Panel content") }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Open Test panel").assertIsDisplayed()
    }

    @Test
    fun panelCanOpenAndCloseRepeatedly() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    val state = rememberSlimRailState()
                    SlimRailPanel(
                        state = state,
                        contentDescription = "Test panel",
                        panelContent = { Text("Panel content") }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Open Test panel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Close Test panel").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Close Test panel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Open Test panel").assertIsDisplayed()

        // Exercise repeated transitions to catch state/anchor regressions.
        repeat(3) {
            composeRule.onNodeWithContentDescription("Open Test panel").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("Close Test panel").assertIsDisplayed()
            composeRule.onNodeWithContentDescription("Close Test panel").performClick()
            composeRule.waitForIdle()
            composeRule.onNodeWithContentDescription("Open Test panel").assertIsDisplayed()
        }
    }

    @Test
    fun customPanelWidthStillRendersAndTransitions() {
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    val state = rememberSlimRailState()
                    SlimRailPanel(
                        state = state,
                        panelWidth = androidx.compose.ui.unit.Dp(260f),
                        railWidth = androidx.compose.ui.unit.Dp(32f),
                        contentDescription = "Resized panel",
                        panelContent = { Text("Resized content") }
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Open Resized panel").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Open Resized panel").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Close Resized panel").assertIsDisplayed()
    }
    @Test
    fun editorBackgroundSwatchesExposeClickableAccessibleActions() {
        var selectedBackground = -1
        composeRule.setContent {
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    EditorToolRail(
                        visible = true,
                        tab = ToolRailTab.Background,
                        onTabChange = {},
                        colorIndex = 0,
                        backgroundIndex = 0,
                        onColorSelected = {},
                        onImportWallpaper = {},
                        onBackgroundSelected = { selectedBackground = it },
                        onApply = {},
                        onCancel = {},
                        onFormat = {}
                    )
                }
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note_background_0")
            .assertIsDisplayed()
            .assertIsSelected()
        composeRule.onNodeWithTag("note_background_1")
            .assertIsDisplayed()
            .assertIsNotSelected()
            .assertHasClickAction()
            .performClick()
        composeRule.waitForIdle()
        org.junit.Assert.assertEquals(1, selectedBackground)
        composeRule.onNodeWithTag("note_background_1").assertIsSelected()
        composeRule.onNodeWithTag("note_background_0").assertIsNotSelected()
    }

}
