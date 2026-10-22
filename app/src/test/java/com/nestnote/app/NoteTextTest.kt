package com.nestnote.app

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NoteTextTest {
    @Test
    fun markdownExportPreservesMeaningfulWhitespaceAndTrailingNewlines() {
        val body = "  indented text\n\ncode  \nlast line\n"
        assertEquals(body, NoteText.toMarkdown("", body))
    }

    @Test
    fun markdownExportPreservesLeadingIndentationWhenConvertingListMarkers() {
        assertEquals("  - item\n\t- [x] done", NoteText.toMarkdown("", "  • item\n\t☑ done"))
    }

    @Test
    fun selectionMappingUsesActualTextEditDeltaRatherThanCursorPosition() {
        val old = "first\nsecond\nthird"
        val new = "first\n☐ second\nthird"
        assertEquals(10 to 15, NoteText.selectionAfterLineEdit(old, new, 8, 13, 10))
    }

    @Test
    fun lineBoundariesSupportLfCrLfAndLoneCr() {
        listOf("\n", "\r\n", "\r").forEach { newline ->
            val text = "first${newline}second${newline}third"
            val secondStart = "first".length + newline.length
            assertEquals(secondStart, NoteText.lineStart(text, secondStart + 2))
            assertEquals(secondStart + "second".length, NoteText.lineEnd(text, secondStart + 2))
        }
    }

    @Test
    fun markdownConversionPreservesNewlineStyleBlankLinesAndTrailingSeparators() {
        listOf("\n", "\r\n", "\r").forEach { newline ->
            val body = "  • item${newline}${newline}code  ${newline}last${newline}"
            assertEquals("  - item${newline}${newline}code  ${newline}last${newline}", NoteText.toMarkdown("", body))
        }
    }

    @Test
    fun enterContinuesListsForLfCrLfAndLoneCr() {
        listOf("\n", "\r\n", "\r").forEach { newline ->
            val old = "  9. nested"
            val new = old + newline
            val expected = "  9. nested${newline}  10. "
            assertEquals(expected to expected.length, NoteText.continueList(old, new, new.length))
        }
    }

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
    fun inlineFormatPreservesReversedSelectionDirectionWhenWrappingAndUnwrapping() {
        val wrapped = NoteText.toggleInlineFormat("hello world", 11, 6, "**")
        assertEquals("hello **world**", wrapped.first)
        assertEquals(13 to 8, wrapped.second to wrapped.third)

        val unwrapped = NoteText.toggleInlineFormat(wrapped.first, wrapped.second, wrapped.third, "**")
        assertEquals("hello world", unwrapped.first)
        assertEquals(11 to 6, unwrapped.second to unwrapped.third)
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
    fun headingLevelsCanBeChangedAndToggledOffWithoutStackingMarkers() {
        val (h2, h2Cursor) = NoteText.toggleHeadingLevel("Title", 3, 2)
        assertEquals("## Title", h2)
        assertEquals(6, h2Cursor)

        val (h3, h3Cursor) = NoteText.toggleHeadingLevel(h2, h2Cursor, 3)
        assertEquals("### Title", h3)
        assertEquals(7, h3Cursor)

        val (plain, plainCursor) = NoteText.toggleHeadingLevel(h3, h3Cursor, 3)
        assertEquals("Title", plain)
        assertEquals(3, plainCursor)
    }

    @Test
    fun headingLevelsPreserveIndentationAndOnlyChangeCurrentLine() {
        val (updated, cursor) = NoteText.toggleHeadingLevel("first\n  title\nlast", 9, 2)
        assertEquals("first\n  ## title\nlast", updated)
        assertEquals(12, cursor)
    }

    @Test
    fun headingLevelRejectsValuesOutsideMarkdownRange() {
        try {
            NoteText.toggleHeadingLevel("Title", 2, 7)
            throw AssertionError("Invalid heading levels must be rejected")
        } catch (_: IllegalArgumentException) {
            // Expected: Markdown heading levels range from 1 through 6.
        }
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
    fun blockquoteTogglePreservesIndentationAndCursor() {
        val (quoted, cursor) = NoteText.toggleBlockquote("  a thought", 6)
        assertEquals("  > a thought", quoted)
        assertEquals(8, cursor)
        val (plain, plainCursor) = NoteText.toggleBlockquote(quoted, cursor)
        assertEquals("  a thought", plain)
        assertEquals(6, plainCursor)
        assertEquals("first\n> second\nthird", NoteText.toggleBlockquote("first\nsecond\nthird", 9).first)
    }

    @Test
    fun markdownLinkInsertionUsesSelectedTextAndPlacesCursorInUrl() {
        val selected = NoteText.insertMarkdownLink("visit site now", 6, 10)
        assertEquals("visit [site](https://) now", selected.first)
        assertEquals(selected.second, selected.third)
        assertEquals("visit [site](https://) now".indexOf("https://") + "https://".length, selected.second)

        val reversed = NoteText.insertMarkdownLink("visit site now", 10, 6)
        assertEquals(selected.first, reversed.first)
        assertEquals(selected.second to selected.third, reversed.second to reversed.third)

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
    fun listActionsReplaceMarkdownBulletPrefixesInsteadOfStackingThem() {
        assertEquals("• item", NoteText.toggleBullet("- item", 4).first)
        assertEquals("• item", NoteText.toggleBullet("* item", 4).first)
        assertEquals("1. item", NoteText.toggleNumberedList("- item", 4).first)
        assertEquals("1. item", NoteText.toggleNumberedList("* item", 4).first)
        assertEquals("☐ item", NoteText.cycleChecklist("- item", 4).first)
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


    @Test
    fun inlineVoiceMarkersSplitTextWithoutLosingSurroundingContent() {
        val marker = "[[voice:3e967866-b886-4db1-abde-4724b7f2bf6c]]"
        assertEquals(
            listOf(
                NoteText.VoiceSegment("before ", null),
                NoteText.VoiceSegment("", "3e967866-b886-4db1-abde-4724b7f2bf6c"),
                NoteText.VoiceSegment(" after", null)
            ),
            NoteText.voiceSegments("before " + marker + " after")
        )
    }

    @Test
    fun adjacentVoiceMarkersRemainDistinctAndKeepTextOrder() {
        val first = "11111111-1111-4111-8111-111111111111"
        val second = "22222222-2222-4222-8222-222222222222"
        val body = "start\n[[voice:$first]][[voice:$second]]\nend"
        assertEquals(
            listOf(
                NoteText.VoiceSegment("start\n", null),
                NoteText.VoiceSegment("", first),
                NoteText.VoiceSegment("", second),
                NoteText.VoiceSegment("\nend", null)
            ),
            NoteText.voiceSegments(body)
        )
        assertEquals(listOf(first, second), NoteText.voiceMarkers(body).map { it.attachmentId })
    }

    @Test
    fun voiceMarkerRemovalPreservesInlineTextAndOtherAttachments() {
        val first = "11111111-1111-4111-8111-111111111111"
        val second = "22222222-2222-4222-8222-222222222222"
        val body = "before [[voice:$first]] middle [[voice:$second]] after"
        assertEquals(
            "before  middle [[voice:$second]] after",
            NoteText.removeVoiceMarker(body, first)
        )
    }


    @Test
    fun voiceMarkersOnlyAcceptUuidIdsAndHideMalformedMarkerPayloads() {
        val valid = "11111111-1111-4111-8111-111111111111"
        val body = "before [[voice:$valid]] middle [[voice:not-a-uuid]] after [[voice:123]]"
        assertEquals(listOf(valid), NoteText.voiceMarkers(body).map { it.attachmentId })
        assertEquals(
            listOf(
                NoteText.VoiceSegment("before ", null),
                NoteText.VoiceSegment("", valid),
                NoteText.VoiceSegment(" middle ", null),
                NoteText.VoiceSegment("Voice note", null),
                NoteText.VoiceSegment(" after ", null),
                NoteText.VoiceSegment("Voice note", null)
            ),
            NoteText.voiceSegments(body)
        )
    }

    @Test
    fun legacyUuidVoiceMarkersRemainBackwardCompatible() {
        val id = "ABCDEF12-3456-7890-abcd-ef1234567890"
        assertEquals(listOf(id), NoteText.voiceMarkers("legacy [[voice:$id]]").map { it.attachmentId })
        assertEquals(
            listOf(NoteText.VoiceSegment("legacy ", null), NoteText.VoiceSegment("", id)),
            NoteText.voiceSegments("legacy [[voice:$id]]")
        )
    }

    @Test
    fun voiceSegmentsPreserveWhitespaceAroundMarkers() {
        val id = "11111111-1111-4111-8111-111111111111"
        val body = "first paragraph\\n\\n[[voice:$id]]\\n\\nlast paragraph"

        assertEquals(
            listOf(
                NoteText.VoiceSegment("first paragraph\\n\\n", null),
                NoteText.VoiceSegment("", id),
                NoteText.VoiceSegment("\\n\\nlast paragraph", null)
            ),
            NoteText.voiceSegments(body)
        )
    }

    @Test
    fun voiceSegmentsPreserveLeadingAndTrailingWhitespaceAndAdjacentMarkers() {
        val first = "11111111-1111-4111-8111-111111111111"
        val second = "22222222-2222-4222-8222-222222222222"
        val body = "\\n[[voice:$first]][[voice:$second]]\\n"

        assertEquals(
            listOf(
                NoteText.VoiceSegment("\\n", null),
                NoteText.VoiceSegment("", first),
                NoteText.VoiceSegment("", second),
                NoteText.VoiceSegment("\\n", null)
            ),
            NoteText.voiceSegments(body)
        )
    }

    @Test
    fun inlineFormattingHandlesAdjacentAndNestedMarkersWithoutEatingNeighborText() {
        val adjacent = NoteText.toggleInlineFormat("ab cd", 0, 2, "**")
        assertEquals("**ab** cd", adjacent.first)
        assertEquals("ab cd", NoteText.toggleInlineFormat(adjacent.first, 2, 4, "**").first)

        val nested = NoteText.toggleInlineFormat("bold italic", 5, 11, "_")
        assertEquals("bold _italic_", nested.first)
        val nestedBold = NoteText.toggleInlineFormat(nested.first, 0, nested.first.length, "**")
        assertEquals("**bold _italic_**", nestedBold.first)
        assertEquals(nested.first, NoteText.toggleInlineFormat(nestedBold.first, 2, nestedBold.first.length - 2, "**").first)
    }

    @Test
    fun inlineFormattingNestsItalicInsideBoldWithoutRemovingExistingMarkers() {
        val bold = NoteText.toggleInlineFormat("bold", 0, 4, "**")
        assertEquals("**bold**", bold.first)

        val italicInsideBold = NoteText.toggleInlineFormat(bold.first, 2, 6, "*")
        assertEquals("***bold***", italicInsideBold.first)
        assertEquals(3 to 7, italicInsideBold.second to italicInsideBold.third)

        val toggledOff = NoteText.toggleInlineFormat(
            italicInsideBold.first,
            italicInsideBold.second,
            italicInsideBold.third,
            "*"
        )
        assertEquals("**bold**", toggledOff.first)
        assertEquals(2 to 6, toggledOff.second to toggledOff.third)
    }

    @Test
    fun numberedListContinuationHandlesDigitBoundariesAndIndentedEmptyItems() {
        val old = "  9. item"
        assertEquals("  9. item\n  10. ", NoteText.continueList(old, "$old\n", old.length + 1)!!.first)
        val parenthesized = "  9) item"
        assertEquals("  9) item\n  10) ", NoteText.continueList(parenthesized, "$parenthesized\n", parenthesized.length + 1)!!.first)
        assertEquals("item", NoteText.toggleNumberedList("9) item", 7).first)

        val empty = "first\n  10. "
        val result = NoteText.continueList(empty, "$empty\n", empty.length + 1)!!
        assertEquals("first\n", result.first)
        assertEquals("first\n".length, result.second)
    }

    @Test
    fun selectionMappingKeepsSelectedTextOutsideInsertedLinePrefixes() {
        val old = "heading"
        val insertedHeading = "## heading"
        assertEquals(3 to 10, NoteText.selectionAfterLineEdit(old, insertedHeading, 0, old.length, 3))

        // If the selection ends exactly where text is inserted, it must not absorb the insertion.
        assertEquals(0 to 1, NoteText.selectionAfterLineEdit("abc", "aXbc", 0, 1, 0))

        // A selection beginning at the insertion point must move after the inserted prefix.
        assertEquals(2 to 4, NoteText.selectionAfterLineEdit("abc", "aXbc", 1, 3, 2))

        // Reversed selection direction is preserved as well.
        assertEquals(4 to 1, NoteText.selectionAfterLineEdit("abc", "aXbc", 3, 1, 2))
    }

    @Test
    fun numberedListTogglePreservesSelectionAcrossPrefixInsertionAndRemoval() {
        val old = "before\n• selected text\nafter"
        val selectionStart = old.indexOf("selected")
        val selectionEnd = selectionStart + "selected text".length
        val (numbered, cursor) = NoteText.toggleNumberedList(old, selectionEnd)
        assertEquals("before\n1. selected text\nafter", numbered)
        val mapped = NoteText.selectionAfterLineEdit(old, numbered, selectionStart, selectionEnd, cursor)
        assertEquals("selected text", numbered.substring(mapped.first, mapped.second))

        val (plain, plainCursor) = NoteText.toggleNumberedList(numbered, cursor)
        val restored = NoteText.selectionAfterLineEdit(numbered, plain, mapped.first, mapped.second, plainCursor)
        assertEquals("selected text", plain.substring(restored.first, restored.second))
    }

    @Test
    fun checklistAndBulletConversionsReplacePrefixesAndKeepIndentationAcrossEnter() {
        assertEquals("  ☐ task", NoteText.cycleChecklist("  - task", 7).first)
        assertEquals("  • task", NoteText.toggleBullet("  * task", 7).first)
        val markdownBullet = "- task"
        assertEquals("- task\n- ", NoteText.continueList(markdownBullet, "$markdownBullet\n", markdownBullet.length + 1)!!.first)
    }

    @Test
    fun markdownLinksPreserveUnicodePunctuationAndDocumentBoundaries() {
        val text = "😀 café, 世界!"
        val start = text.indexOf("café")
        val end = text.indexOf("!", start)
        val linked = NoteText.insertMarkdownLink(text, start, end)
        assertEquals("😀 [café, 世界](https://)!", linked.first)
        assertEquals(linked.first.indexOf("https://") + "https://".length, linked.second)

        val beginning = NoteText.insertMarkdownLink("site", 0, 4)
        assertEquals("[site](https://)", beginning.first)
        val ending = NoteText.insertMarkdownLink("site", 0, 4)
        assertEquals(ending.first.length - 1, ending.second)
    }

    @Test
    fun markdownLinkInsertionAtEmptyDocumentAndIncompleteMarkdownRemainsSafe() {
        val emptyDocument = NoteText.insertMarkdownLink("", 0, 0)
        assertEquals("[text](https://)", emptyDocument.first)
        assertEquals(emptyDocument.first.indexOf("https://") + "https://".length, emptyDocument.second)

        val partial = "[label](https://"
        val linked = NoteText.insertMarkdownLink(partial, 1, 6)
        assertEquals("[[label](https://)](https://", linked.first)
        assertEquals(linked.first.indexOf("https://") + "https://".length, linked.second)
    }


    @Test
    fun inlineFormattingRepeatedTogglesPreserveTextAndSelectionForAllMarkerKinds() {
        listOf("**", "_", "~~", "\u0060").forEach { marker ->
            val original = "left selected right"
            val start = original.indexOf("selected")
            val end = start + "selected".length
            val wrapped = NoteText.toggleInlineFormat(original, start, end, marker)
            assertEquals("left " + marker + "selected" + marker + " right", wrapped.first)
            assertEquals(start + marker.length to end + marker.length, wrapped.second to wrapped.third)

            val unwrapped = NoteText.toggleInlineFormat(wrapped.first, wrapped.second, wrapped.third, marker)
            assertEquals(original, unwrapped.first)
            assertEquals(start to end, unwrapped.second to unwrapped.third)
        }

        val empty = NoteText.toggleInlineFormat("abc", 1, 1, "~~")
        assertEquals("a~~~~bc", empty.first)
        assertEquals(3 to 3, empty.second to empty.third)
    }

    @Test
    fun checklistCyclesPlainUncheckedCheckedAndBackWithoutMovingIndentation() {
        val plain = "  task"
        val unchecked = NoteText.cycleChecklist(plain, plain.length)
        assertEquals("  ☐ task", unchecked.first)
        assertEquals(8, unchecked.second)

        val checked = NoteText.cycleChecklist(unchecked.first, unchecked.second)
        assertEquals("  ☑ task", checked.first)
        assertEquals(8, checked.second)

        val backToPlain = NoteText.cycleChecklist(checked.first, checked.second)
        assertEquals(plain, backToPlain.first)
        assertEquals(6, backToPlain.second)
    }

    @Test
    fun checklistStateUpdatesPreserveLineEndingsAndIgnoreInvalidRows() {
        listOf("\n", "\r\n", "\r").forEach { newline ->
            val body = "  ☐ first" + newline + "☑ second" + newline + "last"
            assertEquals("  ☑ first" + newline + "☑ second" + newline + "last", NoteText.setChecklistItemChecked(body, 0, true))
            assertEquals("  ☐ first" + newline + "☐ second" + newline + "last", NoteText.setChecklistItemChecked(body, 1, false))
            assertEquals(body, NoteText.setChecklistItemChecked(body, 2, true))
            assertEquals(body, NoteText.setChecklistItemChecked(body, 99, true))
        }
    }

    @Test
    fun checklistEnterContinuesUncheckedAndEmptyChecklistItemTerminatesCleanly() {
        val old = "  ☑ done"
        assertEquals("  ☑ done\n  ☐ ", NoteText.continueList(old, old + "\n", old.length + 1)!!.first)

        val empty = "before\n  ☐ "
        val result = NoteText.continueList(empty, empty + "\n", empty.length + 1)!!
        assertEquals("before\n", result.first)
        assertEquals("before\n".length, result.second)
    }

    @Test
    fun markdownLinkEscapesLabelDelimitersAndKeepsUrlCursorCorrect() {
        val text = "a]b\\c"
        val linked = NoteText.insertMarkdownLink(text, 0, text.length)
        assertEquals("[a\\]b\\\\c](https://)", linked.first)
        assertEquals(linked.first.indexOf("https://") + "https://".length, linked.second)
        assertEquals(linked.second, linked.third)

        val noSelection = NoteText.insertMarkdownLink("text", 2, 2)
        assertEquals("te[text](https://)xt", noSelection.first)
        assertEquals(noSelection.first.indexOf("https://") + "https://".length, noSelection.second)
    }

}
