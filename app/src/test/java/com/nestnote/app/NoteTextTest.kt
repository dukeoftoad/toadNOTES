package com.nestnote.app

import java.util.Calendar
import org.junit.Assert.assertEquals
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
}
