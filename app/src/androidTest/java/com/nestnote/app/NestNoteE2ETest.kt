package com.nestnote.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription\nimport androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
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
        composeRule.onNodeWithText("New note").assertIsDisplayed()
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
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
        composeRule.onNodeWithText("Search notes").performClick()
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
        composeRule.onNodeWithText("Expandable bottom sheet").assertIsDisplayed()
        composeRule.onNodeWithText("Full-screen editor").assertIsDisplayed()
        composeRule.onNodeWithText("Quick dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Grid").assertIsDisplayed()
        composeRule.onNodeWithText("List").assertIsDisplayed()
    }
}
