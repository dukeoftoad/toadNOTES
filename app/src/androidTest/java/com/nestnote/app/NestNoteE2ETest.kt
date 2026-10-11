package com.nestnote.app

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
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
        composeRule.onNodeWithText("Create a note").assertIsDisplayed()
        composeRule.onNodeWithText("Note title").performTextInput("E2E note")
        composeRule.onNodeWithText("Your thoughts").performTextInput("Saved by the UI test")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.onNodeWithText("E2E note").assertIsDisplayed()
        composeRule.onNodeWithText("Saved by the UI test").assertIsDisplayed()
    }

    @Test
    fun userCanSearchForANote() {
        composeRule.onNodeWithText("Search").performClick()
        composeRule.onNodeWithText("Search your notes…").assertIsDisplayed()
        composeRule.onNodeWithText("Search your notes…").performTextInput("Trip to the Mountains")
        composeRule.onNodeWithText("Trip to the Mountains").assertIsDisplayed()
        composeRule.onNodeWithText("Project Plan").assertDoesNotExist()
    }

    @Test
    fun userCanOpenAndCancelTheEditor() {
        composeRule.onNodeWithText("New note").performClick()
        composeRule.onNodeWithText("Create a note").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Create a note").assertDoesNotExist()
    }
}
