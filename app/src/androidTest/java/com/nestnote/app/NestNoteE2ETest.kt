package com.nestnote.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NestNoteE2ETest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun clearLocalNotes() {
        composeRule.activity.getSharedPreferences("nestnote_local", 0)
            .edit().clear().commit()
        composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
            .edit().clear().commit()
        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }
        composeRule.waitForIdle()
    }

    @Test
    fun dashboardShowsStarterNotesAndNewNoteAction() {
        composeRule.onNodeWithText("NESTNOTE").assertIsDisplayed()
        composeRule.onNodeWithText("Capture what matters.").assertIsDisplayed()
        composeRule.onNodeWithText("Your notes").assertIsDisplayed()
        composeRule.onNodeWithText("All notes").assertIsDisplayed()
        composeRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeRule.onNodeWithText("New note").assertIsDisplayed()
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
    }

    @Test
    fun layoutTogglePersistsListSelection() {
        composeRule.onNodeWithContentDescription("Switch dashboard layout").performClick()
        composeRule.runOnIdle {
            assertEquals(
                "LIST",
                composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
                    .getString("dashboard_layout", null)
            )
        }
    }

    @Test
    fun userCanCreateAndSaveANote() {
        composeRule.onNodeWithText("New note").performClick()
        composeRule.onNodeWithText("Capture it before it slips away.").assertIsDisplayed()
        composeRule.onNodeWithText("Title").performTextInput("E2E note")
        composeRule.onNodeWithText("Your note").performTextInput("Saved by the UI test")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.onNodeWithText("E2E note").assertIsDisplayed()
        composeRule.onNodeWithText("Saved by the UI test").assertIsDisplayed()
    }

    @Test
    fun userCanSearchForANote() {
        composeRule.onNodeWithContentDescription("Search notes").performClick()
        composeRule.onNodeWithText("Search notes").assertIsDisplayed()
    }

    @Test
    fun userCanOpenAndCancelTheEditor() {
        composeRule.onNodeWithText("New note").performClick()
        composeRule.onNodeWithText("Capture it before it slips away.").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onAllNodesWithText("Capture it before it slips away.").assertCountEquals(0)
    }

    @Test
    fun settingsExposeEditorLayoutAndWallpaperColorChoices() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Wallpaper colors").assertIsDisplayed()
        val settings = composeRule.onNodeWithTag("settings_scroll")
        settings.performScrollToNode(hasText("Grid"))
        composeRule.onNodeWithText("Grid").assertIsDisplayed()
        composeRule.onNodeWithText("List").assertIsDisplayed()
        settings.performScrollToNode(hasText("Comfortable"))
        composeRule.onNodeWithText("Dashboard density").assertIsDisplayed()
        composeRule.onNodeWithText("Compact").assertIsDisplayed()
        composeRule.onNodeWithText("Comfortable").assertIsDisplayed()
        settings.performScrollToNode(hasText("Expandable bottom sheet"))
        composeRule.onNodeWithText("Expandable bottom sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Full-screen editor").assertIsDisplayed()
        composeRule.onNodeWithText("Quick dialog").assertIsDisplayed()
    }
}
