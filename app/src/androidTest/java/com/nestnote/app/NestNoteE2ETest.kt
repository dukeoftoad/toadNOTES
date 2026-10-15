package com.nestnote.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextClearance
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
        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }
        composeRule.waitForIdle()
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
    fun settingsCardMetadataTogglesUpdatePreferencesOnceAndPersist() {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Show note previews").assertIsDisplayed()
        composeRule.onNodeWithText("Show note previews").performClick()
        composeRule.onNodeWithText("Show dates and reminders").assertIsDisplayed()
        composeRule.onNodeWithText("Show dates and reminders").performClick()

        composeRule.runOnIdle {
            val preferences = composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
            assertEquals(false, preferences.getBoolean("show_note_previews", true))
            assertEquals(false, preferences.getBoolean("show_note_timestamps", true))
        }

        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Show note previews").assertIsDisplayed()
        composeRule.onNodeWithText("Show dates and reminders").assertIsDisplayed()
        composeRule.runOnIdle {
            val preferences = composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
            assertEquals(false, preferences.getBoolean("show_note_previews", true))
            assertEquals(false, preferences.getBoolean("show_note_timestamps", true))
        }
    }

    @Test
    fun systemBackReturnsFromSettingsToHome() {
        composeRule.onNodeWithContentDescription("Open settings").performClick()
        composeRule.onNodeWithText("Backup & privacy").assertIsDisplayed()

        composeRule.activity.onBackPressedDispatcher.onBackPressed()
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Your notes").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Create note").assertIsDisplayed()
    }

    @Test
    fun categorySelectionFiltersDashboardNotes() {
        composeRule.onNodeWithText("Categories").performClick()
        composeRule.onNodeWithText("Work").performClick()
        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
        composeRule.onAllNodesWithText("Trip to the Mountains").assertCountEquals(0)
    }

    @Test
    fun searchMatchesNoteBodyAndCategoryCaseInsensitively() {
        composeRule.onNodeWithTag("note_search_field").performTextInput("quiet trail")
        composeRule.onNodeWithText("Trip to the Mountains").assertIsDisplayed()
        composeRule.onAllNodesWithText("Project Plan").assertCountEquals(0)

        composeRule.onNodeWithTag("note_search_field").performTextClearance()
        composeRule.onNodeWithText("Search your notes").performTextInput("PERSONAL")
        composeRule.onNodeWithText("Trip to the Mountains").assertIsDisplayed()
        composeRule.onAllNodesWithText("Project Plan").assertCountEquals(0)
    }

    @Test
    fun searchWithNoMatchesShowsHelpfulEmptyState() {
        composeRule.onNodeWithText("Search your notes").performTextInput("no-note-should-match-this")
        composeRule.onNodeWithText("No matching notes").assertIsDisplayed()
        composeRule.onNodeWithText("Try another search, or clear it to see all your notes.").assertIsDisplayed()
        composeRule.onNodeWithText("Clear search").assertIsDisplayed()
    }

    @Test
    fun appearancePickerChangesAndPersistsColorAndWallpaper() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithText("Colour palette").assertIsDisplayed()
        composeRule.onNodeWithText("Backgrounds").assertIsDisplayed()
        composeRule.onNodeWithTag("note_color_2").performClick()
        composeRule.onNodeWithTag("note_color_2").assertIsSelected()
        composeRule.onNodeWithTag("note_background_1").performClick()
        composeRule.onNodeWithTag("note_background_1").assertIsSelected()
        composeRule.onNodeWithText("Apply").performClick()
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
    fun cancellingAppearanceRestoresOriginalDraftSelection() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithTag("note_color_2").performClick()
        composeRule.onNodeWithTag("note_background_1").performClick()
        composeRule.onNodeWithTag("appearance_cancel").performClick()
        composeRule.onNodeWithText("Title").performTextInput("Cancelled appearance")
        composeRule.onNodeWithText("Note").performTextInput("Appearance should stay default")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.runOnIdle {
            val saved = com.nestnote.app.NoteStore(composeRule.activity).load().first { it.title == "Cancelled appearance" }
            assertEquals(0, saved.color)
            assertEquals(0, saved.background)
        }
    }

    @Test
    fun closingAppearancePanelRestoresOriginalDraftSelection() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithTag("note_color_3").performClick()
        composeRule.onNodeWithTag("note_background_2").performClick()
        composeRule.onNodeWithTag("appearance_close").performClick()
        composeRule.onNodeWithText("Title").performTextInput("Closed appearance")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.runOnIdle {
            val saved = com.nestnote.app.NoteStore(composeRule.activity).load().first { it.title == "Closed appearance" }
            assertEquals(0, saved.color)
            assertEquals(0, saved.background)
        }
    }

    @Test
    fun systemBackClosesAppearanceAndRestoresDraftSelection() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithTag("note_color_4").performClick()
        composeRule.onNodeWithTag("note_background_3").performClick()

        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Colour palette").assertDoesNotExist()
        composeRule.onNodeWithText("Title").performTextInput("Back cancelled appearance")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.runOnIdle {
            val saved = NoteStore(composeRule.activity).load().first { it.title == "Back cancelled appearance" }
            assertEquals(0, saved.color)
            assertEquals(0, saved.background)
        }
    }

    @Test
    fun tappingAppearanceScrimClosesPanelAndRestoresDraftSelection() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithTag("note_color_5").performClick()
        composeRule.onNodeWithTag("note_background_4").performClick()
        composeRule.onNodeWithTag("appearance_scrim").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Colour palette").assertDoesNotExist()
        composeRule.onNodeWithText("Title").performTextInput("Scrim cancelled appearance")
        composeRule.onNodeWithText("Save note").performClick()
        composeRule.runOnIdle {
            val saved = NoteStore(composeRule.activity).load().first { it.title == "Scrim cancelled appearance" }
            assertEquals(0, saved.color)
            assertEquals(0, saved.background)
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

    @Test
    fun settingsExposeEditorTextSizeAndBackup() {
        composeRule.onNodeWithText("Settings").performClick()
        val settings = composeRule.onNodeWithTag("settings_scroll")
        settings.performScrollToNode(hasText("Text size"))
        composeRule.onNodeWithText("Large").performClick()
        composeRule.runOnIdle {
            val prefs = composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
            assertEquals("LARGE", prefs.getString("editor_text_size", null))
        }
        settings.performScrollToNode(hasText("Back up notes"))
        composeRule.onNodeWithText("Back up notes").assertIsDisplayed()
        composeRule.onNodeWithText("Restore backup").assertIsDisplayed()
    }

    @Test
    fun quickDialogUsesSharedEditorAndAppearanceFlow() {
        composeRule.onNodeWithText("Settings").performClick()
        val settings = composeRule.onNodeWithTag("settings_scroll")
        settings.performScrollToNode(hasText("Quick dialog"))
        composeRule.onNodeWithText("Quick dialog").performClick()
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithText("Title").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Choose note appearance").performClick()
        composeRule.onNodeWithText("Colour palette").assertIsDisplayed()
        composeRule.onNodeWithTag("appearance_cancel").performClick()
        composeRule.onNodeWithText("Cancel").performClick()
    }

    @Test
    fun editorChecklistButtonPrefixesTheCurrentLine() {
        composeRule.onNodeWithContentDescription("Create note").performClick()
        composeRule.onNodeWithText("Note").performTextInput("buy milk")
        composeRule.onNodeWithContentDescription("Toggle checklist item").performClick()
        composeRule.onNodeWithText("☐ buy milk").assertIsDisplayed()
    }
}
