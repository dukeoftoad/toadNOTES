package com.nestnote.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    @Test
    fun settingsItemKeysRemainUniqueAndSortChoicesRemainStable() {
        assertTrue(SettingsItemKeys.isNotEmpty())
        assertTrue("Settings LazyColumn keys must be unique", SettingsItemKeys.size == SettingsItemKeys.toSet().size)
        assertTrue(SortOptions.any { it.first == "RECENT" })
        assertTrue(SortOptions.any { it.first == "CREATED" })
        assertTrue(SortOptions.any { it.first == "TITLE" })
        assertTrue("Sort keys must be unique", SortOptions.map { it.first }.size == SortOptions.map { it.first }.toSet().size)
    }

    @Test
    fun blankQueryKeepsEverySectionVisible() {
        SettingsSearchSections.forEach { section ->
            assertTrue("$section should match a blank query", matchesSettingsSearch("  ", section))
        }
    }

    @Test
    fun matchingIsCaseInsensitiveAndSupportsPartialKeywords() {
        assertTrue(matchesSettingsSearch("THEME", "appearance"))
        assertTrue(matchesSettingsSearch("wav", "voice"))
        assertTrue(matchesSettingsSearch("mark", "editor"))
    }

    @Test
    fun keywordSearchCanMatchWhenQueryContainsKeyword() {
        assertTrue(matchesSettingsSearch("pure black dark mode", "appearance"))
        assertTrue(matchesSettingsSearch("privacy settings", "privacy"))
    }

    @Test
    fun unrelatedQueryDoesNotMatchSection() {
        assertFalse(matchesSettingsSearch("watermelon", "appearance"))
        assertFalse(matchesSettingsSearch("theme", "voice"))
        assertFalse(matchesSettingsSearch("theme", "unknown"))
    }
}
