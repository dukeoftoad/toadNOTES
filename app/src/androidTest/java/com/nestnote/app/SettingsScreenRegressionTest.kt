package com.nestnote.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class SettingsScreenRegressionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsSearchFiltersSectionsWithoutRemovingSearchControl() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeChange = {},
                    dynamicColors = true,
                    onDynamicColorsChange = {},
                    amoled = false,
                    onAmoledChange = {},
                    dashboardLayout = DashboardLayout.GRID,
                    onLayoutChange = {},
                    editorMode = EditorPresentationMode.SHEET,
                    onEditorModeChange = {},
                    dashboardDensity = DashboardDensity.COMFORTABLE,
                    onDensityChange = {},
                    noteColorStyle = NoteColorStyle.PASTEL,
                    onNoteColorStyleChange = {},
                    sortKey = "RECENT",
                    onSortChange = {},
                    showNotePreviews = true,
                    onShowNotePreviewsChange = {},
                    showNoteTimestamps = true,
                    onShowNoteTimestampsChange = {},
                    defaultNoteCategory = "Ideas",
                    onDefaultNoteCategoryChange = {},
                    editorTextSize = EditorTextSize.MEDIUM,
                    onEditorTextSizeChange = {},
                    showEditorCounts = true,
                    onShowEditorCountsChange = {},
                    onResetDefaults = {},
                    totalNotes = 4,
                    archivedNotes = 1
                )
            }
        }

        composeRule.onNodeWithTag("settings_search").assertExists()
        composeRule.onNodeWithText("Appearance").assertExists()

        composeRule.onNodeWithTag("settings_search").performTextInput("editor")
        composeRule.onNodeWithTag("settings_search").assertExists()
        composeRule.onNodeWithText("Note editor").assertExists()
        composeRule.onNodeWithText("Appearance").assertDoesNotExist()
    }

    @Test
    fun unknownSortPreferenceFallsBackToSupportedDefaultWithoutCrashing() {
        composeRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    themeMode = ThemeMode.SYSTEM,
                    onThemeChange = {},
                    dynamicColors = false,
                    onDynamicColorsChange = {},
                    amoled = false,
                    onAmoledChange = {},
                    dashboardLayout = DashboardLayout.GRID,
                    onLayoutChange = {},
                    editorMode = EditorPresentationMode.SHEET,
                    onEditorModeChange = {},
                    dashboardDensity = DashboardDensity.COMFORTABLE,
                    onDensityChange = {},
                    noteColorStyle = NoteColorStyle.PASTEL,
                    onNoteColorStyleChange = {},
                    sortKey = "REMOVED_OLD_SORT_OPTION",
                    onSortChange = {},
                    showNotePreviews = true,
                    onShowNotePreviewsChange = {},
                    showNoteTimestamps = true,
                    onShowNoteTimestampsChange = {},
                    defaultNoteCategory = "Ideas",
                    onDefaultNoteCategoryChange = {},
                    editorTextSize = EditorTextSize.MEDIUM,
                    onEditorTextSizeChange = {},
                    showEditorCounts = true,
                    onShowEditorCountsChange = {},
                    onResetDefaults = {},
                    totalNotes = 0,
                    archivedNotes = 0
                )
            }
        }

        composeRule.onNodeWithTag("settings_scroll").assertExists()
        composeRule.onNodeWithText("Settings").assertExists()
    }
}
