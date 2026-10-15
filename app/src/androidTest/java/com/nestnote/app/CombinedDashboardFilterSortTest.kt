package com.nestnote.app

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression coverage for combining dashboard filters, search, and sorting.
 * These tests compile in required CI and run with connectedDebugAndroidTest
 * on an available local device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class CombinedDashboardFilterSortTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetLocalState() {
        composeRule.activity.getSharedPreferences("nestnote_local", 0)
            .edit().clear().commit()
        composeRule.activity.getSharedPreferences("nestnote_preferences", 0)
            .edit().clear().commit()
        composeRule.activity.runOnUiThread { composeRule.activity.recreate() }
        composeRule.waitForIdle()
    }

    @Test
    fun categoryFilterAndTitleSortKeepOnlyMatchingNotes() {
        composeRule.onNodeWithText("Categories").performClick()
        composeRule.onNodeWithText("Work").performClick()

        composeRule.onNodeWithText("Recently edited").performClick()
        composeRule.onNodeWithText("Title A–Z").performClick()

        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
        composeRule.onAllNodesWithText("Trip to the Mountains").assertCountEquals(0)
        composeRule.runOnIdle {
            val preferences = composeRule.activity
                .getSharedPreferences("nestnote_preferences", 0)
            assertEquals("TITLE", preferences.getString("default_sort", null))
        }
    }

    @Test
    fun searchAndCategoryFilterCanBeAppliedTogether() {
        composeRule.onNodeWithText("Categories").performClick()
        composeRule.onNodeWithText("Work").performClick()
        composeRule.onNodeWithTag("note_search_field").performTextInput("Project")

        composeRule.onNodeWithText("Project Plan").assertIsDisplayed()
        composeRule.onAllNodesWithText("Trip to the Mountains").assertCountEquals(0)
    }
}
