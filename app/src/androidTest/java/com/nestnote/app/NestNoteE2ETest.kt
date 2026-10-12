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
        composeRule.onNodeWithText("NestNote").assertIsDisplayed()
        composeRule.onNodeWithText("Your thoughts, organized.").assertIsDisplayed()
        composeRule.onNodeWithText("Search your notes").assertIsDisplayed()
        composeRule.onNodeWithText("Your notes").assertIsDisplayed()
        composeRule.onNodeWithText("All notes").assertIsDisplayed()
        composeRule.onNodeWithText("Reminders").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create note").assertIsDisplayed()
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
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithText("Title").assertIsDisplayed()
        composeRule.onNodeWithText("Title").performTextInput("E2E note")
        composeRule.onNodeWithText("Note").performTextInput("Saved by the UI test")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.onNodeWithText("E2E note").assertIsDisplayed()
        composeRule.onNodeWithText("Saved by the UI test").assertIsDisplayed()
    }

    @Test
    fun homeScreenOffersSortingOptions() {
        composeRule.onNodeWithText("Recently edited").performClick()
        composeRule.onNodeWithText("Title A–Z").assertIsDisplayed()
        composeRule.onNodeWithText("Title A–Z").performClick()
        composeRule.onNodeWithText("Your notes").assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(
                "TITLE",
                composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
                    .getString("default_sort", null)
            )
        }
        composeRule.onNodeWithText("Title A–Z").performClick()
        composeRule.onNodeWithText("Recently created").assertIsDisplayed()
        composeRule.onNodeWithText("Recently created").performClick()
        composeRule.runOnIdle {
            assertEquals(
                "CREATED",
                composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
                    .getString("default_sort", null)
            )
        }
    }


    @Test
    fun archivedNoteCanBeRestoredFromArchivedView() {
        composeRule.onNodeWithContentDescription("Actions for Project Plan").performClick()
        composeRule.onNodeWithText("Archive").performClick()
        composeRule.onNodeWithText("Archived").performClick()
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Actions for Project Plan").performClick()
        composeRule.onNodeWithText("Restore from archive").performClick()
        composeRule.onAllNodesWithText("Project Plan").assertCountEquals(0)
        composeRule.onNodeWithText("Home").performClick()
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
    }

    @Test
    fun userCanSearchForANote() {
        composeRule.onNodeWithText("Search your notes").performTextInput("Project")
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
        composeRule.onAllNodesWithText("Trip to the Mountains").assertCountEquals(0)
    }

    @Test
    fun cancelWithUnsavedChangesRequiresConfirmation() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithText("Title").performTextInput("Draft not saved")
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Discard changes?").assertIsDisplayed()
        composeRule.onNodeWithText("Keep editing").performClick()
        composeRule.onNodeWithText("Draft not saved").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithText("Discard").performClick()
        composeRule.onAllNodesWithText("Title").assertCountEquals(0)
    }

    @Test
    fun userCanOpenAndCancelTheEditor() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithText("Title").assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onAllNodesWithText("Title").assertCountEquals(0)
    }

    @Test
    fun categorySelectionFiltersDashboardNotes() {
        composeRule.onNodeWithText("Categories").performClick()
        composeRule.onNodeWithText("Work").performClick()
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
        composeRule.onAllNodesWithText("Trip to the Mountains").assertCountEquals(0)
    }

    @Test
    fun searchWithNoMatchesShowsHelpfulEmptyState() {
        composeRule.onNodeWithText("Search your notes").performTextInput("no-note-should-match-this")
        composeRule.onNodeWithText("A fresh page awaits").assertIsDisplayed()
        composeRule.onNodeWithText("Create a note and let ideas flow.").assertIsDisplayed()
    }

    @Test
    fun appearancePickerChangesAndPersistsColorAndWallpaper() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithText("Colour").assertIsDisplayed()
        composeRule.onNodeWithText("Background").assertIsDisplayed()
        composeRule.onNodeWithTag("note_color_2").performClick()
        composeRule.onNodeWithTag("note_background_1").performClick()
        composeRule.onNodeWithText("Done").performClick()
        composeRule.onNodeWithText("Title").performTextInput("Wallpaper note")
        composeRule.onNodeWithText("Note").performTextInput("Illustrated background")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.runOnIdle {
            val saved = com.nestnote.app.NoteStore(composeRule.activity).load().first { it.title == "Wallpaper note" }
            assertEquals(2, saved.color)
            assertEquals(1, saved.background)
        }
    }

    @Test
    fun settingsExposeEditorLayoutAndWallpaperColorChoices() {
        composeRule.onNodeWithText("Settings").performClick()
        composeRule.onNodeWithText("Wallpaper colors").assertIsDisplayed()
        val settings = composeRule.onNodeWithTag("settings_scroll")
        settings.performScrollToNode(hasText("Soft pastel cards"))
        composeRule.onNodeWithText("Soft pastel cards").assertIsDisplayed()
        composeRule.onNodeWithText("Match wallpaper").assertIsDisplayed()
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
