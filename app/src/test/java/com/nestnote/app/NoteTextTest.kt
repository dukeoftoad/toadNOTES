package com.nestnote.app

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NoteTextTest {
    @Test
    fun wordCountIgnoresExtraWhitespace() {
        assertEquals(0, NoteText.wordCount("   \n "))
        assertEquals(3, NoteText.wordCount("one  two\nthree "))
    }

    @Test
    fun readingMinutesRoundsUpAndIsZeroForEmpty() {
        assertEquals(0, NoteText.readingMinutes(0))
        assertEquals(1, NoteText.readingMinutes(5))
        assertEquals(1, NoteText.readingMinutes(200))
        assertEquals(2, NoteText.readingMinutes(201))
    }

    @Test
    fun inlineFormatWrapsSelectionAndCanRemoveExistingMarkers() {
        val wrapped = NoteText.toggleInlineFormat("hello world", 6, 11, "**")
        assertEquals("hello **world**", wrapped.first)
        assertEquals(8 to 13, wrapped.second to wrapped.third)
        val unwrapped = NoteText.toggleInlineFormat(wrapped.first, wrapped.second, wrapped.third, "**")
        assertEquals("hello world", unwrapped.first)
        assertEquals(6 to 11, unwrapped.second to unwrapped.third)
    }

    @Test
    fun inlineFormatInsertsMarkerPairAtCollapsedCursor() {
        val result = NoteText.toggleInlineFormat("abcd", 2, 2, "_")
        assertEquals("ab__cd", result.first)
        assertEquals(3 to 3, result.second to result.third)
    }

    @Test
    fun inlineFormattingSupportsStrikethroughAndInlineCodeMarkers() {
        val struck = NoteText.toggleInlineFormat("not this", 4, 8, "~~")
        assertEquals("not ~~this~~", struck.first)
        assertEquals("not this", NoteText.toggleInlineFormat(struck.first, struck.second, struck.third, "~~").first)

        val code = NoteText.toggleInlineFormat("run command", 4, 11, "`")
        assertEquals("run `command`", code.first)
        assertEquals("run command", NoteText.toggleInlineFormat(code.first, code.second, code.third, "`").first)
    }

    @Test
    fun inlineFormattingClampsSelectionAndRejectsEmptyMarker() {
        val result = NoteText.toggleInlineFormat("abc", -10, 20, "**")
        assertEquals("**abc**", result.first)
        assertEquals(2 to 5, result.second to result.third)
        try {
            NoteText.toggleInlineFormat("abc", 0, 1, "")
            throw AssertionError("An empty marker must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: empty formatting markers would make toggling ambiguous.
        }
    }

    @Test
    fun headingToggleAddsAndRemovesPrefixWithoutChangingOtherLines() {
        assertEquals("# Heading\nbody", NoteText.toggleHeading("Heading\nbody", 3).first)
        assertEquals("Heading\nbody", NoteText.toggleHeading("# Heading\nbody", 5).first)
    }

    @Test
    fun headingTogglePreservesIndentationAndCursorPosition() {
        val (heading, cursor) = NoteText.toggleHeading("  title", 5)
        assertEquals("  # title", heading)
        assertEquals(7, cursor)
        val (plain, plainCursor) = NoteText.toggleHeading(heading, cursor)
        assertEquals("  title", plain)
        assertEquals(5, plainCursor)
    }

    @Test
    fun markdownLinkInsertionUsesSelectedTextAndPlacesCursorInUrl() {
        val selected = NoteText.insertMarkdownLink("visit site now", 6, 10)
        assertEquals("visit [site](https://) now", selected.first)
        assertEquals(selected.second, selected.third)
        assertEquals("visit [site](https://) now".indexOf("https://") + "https://".length, selected.second)

        val empty = NoteText.insertMarkdownLink("abc", 1, 1)
        assertEquals("a[text](https://)bc", empty.first)
        assertEquals(empty.first.indexOf("https://") + "https://".length, empty.second)
    }

    @Test
    fun numberedListToggleAddsAndRemovesPrefixWithoutChangingIndentation() {
        val (numbered, cursor) = NoteText.toggleNumberedList("  first", 7)
        assertEquals("  1. first", numbered)
        assertEquals(10, cursor)
        val (plain, plainCursor) = NoteText.toggleNumberedList(numbered, cursor)
        assertEquals("  first", plain)
        assertEquals(7, plainCursor)
    }

    @Test
    fun numberedListToggleOnlyChangesCurrentLineAndReplacesOtherListPrefix() {
        assertEquals("☐ first\n1. second\nthird", NoteText.toggleNumberedList("☐ first\n• second\nthird", 12).first)
    }

    @Test
    fun enterContinuesNumberedListsAndIncrementsTheNumber() {
        val old = "1. first"
        assertEquals("1. first\n2. ", NoteText.continueList(old, "$old\n", old.length + 1)!!.first)
        val second = "  9. nested"
        assertEquals("  9. nested\n  10. ", NoteText.continueList(second, "$second\n", second.length + 1)!!.first)
    }

    @Test
    fun checklistCyclesThroughUncheckedCheckedAndPlain() {
        val (unchecked, c1) = NoteText.cycleChecklist("buy milk", 8)
        assertEquals("☐ buy milk", unchecked)
        assertEquals(10, c1)
        val (checked, _) = NoteText.cycleChecklist(unchecked, c1)
        assertEquals("☑ buy milk", checked)
        val (plain, c3) = NoteText.cycleChecklist(checked, 10)
        assertEquals("buy milk", plain)
        assertEquals(8, c3)
    }

    @Test
    fun checklistOnlyAffectsTheCursorLine() {
        val (text, _) = NoteText.cycleChecklist("first\nsecond\nthird", 8)
        assertEquals("first\n☐ second\nthird", text)
    }

    @Test
    fun bulletReplacesCheckboxAndTogglesOff() {
        val (bullet, _) = NoteText.toggleBullet("☐ item", 3)
        assertEquals("• item", bullet)
        val (plain, _) = NoteText.toggleBullet(bullet, 3)
        assertEquals("item", plain)
    }

    @Test
    fun enterContinuesBulletAndChecklistItems() {
        // "• milk" with Enter pressed at the end -> new bullet
        val bullet = NoteText.continueList("• milk", "• milk\n", 7)
        assertEquals("• milk\n• " to 9, bullet)
        // A checked item continues as an unchecked one
        val check = NoteText.continueList("☑ done", "☑ done\n", 7)
        assertEquals("☑ done\n☐ " to 9, check)
    }

    @Test
    fun enterOnEmptyListItemEndsTheList() {
        val result = NoteText.continueList("• milk\n• ", "• milk\n• \n", 10)
        assertEquals("• milk\n" to 7, result)
    }

    @Test
    fun enterOnPlainLineOrPastedTextDoesNothing() {
        assertNull(NoteText.continueList("plain", "plain\n", 6))
        assertNull(NoteText.continueList("• a", "• a\n\nb", 4))
    }

    @Test
    fun lineFormattingPreservesSelectedTextRange() {
        val old = "first\nsecond"
        val (newText, cursor) = NoteText.cycleChecklist(old, 8)
        assertEquals("first\n☐ second", newText)
        assertEquals(10 to 14, NoteText.selectionAfterLineEdit(old, newText, 8, 12, cursor))
    }

    @Test
    fun lineFormattingKeepsCollapsedSelectionAtReturnedCursor() {
        assertEquals(6 to 6, NoteText.selectionAfterLineEdit("item", "☐ item", 4, 4, 6))
    }

    @Test
    fun interactiveChecklistUpdatesOnlySelectedItemAndPreservesIndentation() {
        val body = "plain\n  ☐ first\n☑ second"
        assertEquals(
            listOf(
                NoteText.ChecklistItem(1, "first", false),
                NoteText.ChecklistItem(2, "second", true)
            ),
            NoteText.checklistItems(body)
        )
        assertEquals("plain\n  ☑ first\n☑ second", NoteText.setChecklistItemChecked(body, 1, true))
        assertEquals("plain\n  ☐ first\n☐ second", NoteText.setChecklistItemChecked(body, 2, false))
        assertEquals(body, NoteText.setChecklistItemChecked(body, 0, true))
        assertEquals(body, NoteText.setChecklistItemChecked(body, 99, true))
    }

    @Test
    fun interactiveChecklistIsIdempotentAndPreservesTrailingNewline() {
        val body = "☐ first\nplain\n"
        val checked = NoteText.setChecklistItemChecked(body, 0, true)
        assertEquals("☑ first\nplain\n", checked)
        assertEquals(checked, NoteText.setChecklistItemChecked(checked, 0, true))
        assertEquals(body, NoteText.setChecklistItemChecked(checked, 0, false))
        assertEquals(emptyList<NoteText.ChecklistItem>(), NoteText.checklistItems("\nplain\n• bullet"))
    }

    @Test
    fun checklistProgressCountsOnlyChecklistLines() {
        assertEquals(0 to 0, NoteText.checklistProgress("plain\n• bullet"))
        assertEquals(3 to 1, NoteText.checklistProgress("☑ done\n☐ next\n  ☐ indented\nplain"))
    }

    @Test
    fun shareTextJoinsNonBlankParts() {
        assertEquals("Title\n\nBody", NoteText.shareText(" Title ", " Body "))
        assertEquals("Body", NoteText.shareText("", "Body"))
    }

    @Test
    fun reminderPresetsLandOnExpectedTimes() {
        // Wednesday 2026-10-14 15:20
        val now = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 14, 15, 20, 0) }
        assertEquals("2026-10-14 18:00", NoteText.formatReminder(NoteText.laterToday(now)))
        assertEquals("2026-10-15 09:00", NoteText.formatReminder(NoteText.tomorrowMorning(now)))
        assertEquals("2026-10-19 09:00", NoteText.formatReminder(NoteText.nextMonday(now)))
        // On a Monday, "next Monday" is a week away, never today.
        val monday = Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 12, 8, 0, 0) }
        assertEquals("2026-10-19 09:00", NoteText.formatReminder(NoteText.nextMonday(monday)))
        // The input calendar is not mutated.
        assertEquals(14, now.get(Calendar.DAY_OF_MONTH))
    }
 
    @Test
    fun checklistActionOnIndentedItemReplacesPrefixInsteadOfStackingIt() {
        // The checklist view and progress count already accept indented items.
        val indented = "  ☐ buy milk"
        val (checked, cursor) = NoteText.cycleChecklist(indented, 8)
        assertEquals("  ☑ buy milk", checked)
        assertEquals(true, cursor >= 4)
        val (plain, _) = NoteText.cycleChecklist(checked, cursor)
        assertEquals("  buy milk", plain)
        val (restarted, _) = NoteText.cycleChecklist(plain, 4)
        assertEquals("  ☐ buy milk", restarted)
        // Only the cursor's line changes in multi-line text.
        assertEquals("a\n\t☑ b\nc", NoteText.cycleChecklist("a\n\t☐ b\nc", 5).first)
    }

    @Test
    fun bulletToggleOnIndentedItemDoesNotStackPrefixes() {
        assertEquals("  • x", NoteText.toggleBullet("  x", 3).first)
        assertEquals("  x", NoteText.toggleBullet("  • x", 4).first)
        // A bullet replaces an indented checkbox rather than sitting beside it.
        assertEquals("  • x", NoteText.toggleBullet("  ☐ x", 4).first)
    }

    @Test
    fun enterContinuesIndentedListItemsAtTheSameIndentation() {
        val old = "  • nested"
        val (text, cursor) = NoteText.continueList(old, "$old\n", old.length + 1)!!
        assertEquals("  • nested\n  • ", text)
        assertEquals(text.length, cursor)
        val checklist = "\t☑ done"
        assertEquals("\t☑ done\n\t☐ ", NoteText.continueList(checklist, "$checklist\n", checklist.length + 1)!!.first)
    }

    @Test
    fun enterOnEmptyIndentedListItemEndsTheListAndClearsTheLine() {
        val old = "a\n  • "
        val (text, cursor) = NoteText.continueList(old, "$old\n", old.length + 1)!!
        assertEquals("a\n", text)
        assertEquals(2, cursor)
    }

    @Test
    fun indentedChecklistItemsStayConsistentAcrossViewProgressAndActions() {
        val text = "  ☐ one\n  ☑ two"
        assertEquals(2, NoteText.checklistItems(text).size)
        assertEquals(2 to 1, NoteText.checklistProgress(text))
        val toggled = NoteText.cycleChecklist(text, 3).first
        assertEquals(2 to 2, NoteText.checklistProgress(toggled))
    }

    @Test
    fun laterTodayIsOfferedOnlyWhenItStaysOnTheSameDay() {
        fun at(hour: Int, minute: Int) =
            Calendar.getInstance().apply { clear(); set(2026, Calendar.OCTOBER, 14, hour, minute, 0) }
        assertEquals("2026-10-14 13:00", NoteText.formatReminder(NoteText.laterTodayOrNull(at(10, 5))!!))
        assertEquals("2026-10-14 23:00", NoteText.formatReminder(NoteText.laterTodayOrNull(at(20, 59))!!))
        // 21:00 + 3h is midnight, which is tomorrow: "Later today" must not be offered.
        assertNull(NoteText.laterTodayOrNull(at(21, 0)))
        assertNull(NoteText.laterTodayOrNull(at(23, 59)))
        // Year boundary.
        val newYearsEve = Calendar.getInstance().apply { clear(); set(2026, Calendar.DECEMBER, 31, 22, 0, 0) }
        assertNull(NoteText.laterTodayOrNull(newYearsEve))
        assertNotNull(NoteText.laterTodayOrNull(Calendar.getInstance().apply { clear(); set(2026, Calendar.DECEMBER, 31, 8, 0, 0) }))
    }

}
