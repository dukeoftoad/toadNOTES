# NestNote Redesign Prototype Parity Audit

Date: 2026-10-19
Reference: `NestNote redesign prototype.html`
Scope: Compare the supplied HTML prototype with the current Jetpack Compose implementation. This is a source audit; no Android device or emulator was connected, so visual, keyboard, accessibility, and motion findings remain unverified until manual validation.

## Executive Summary

The main redesign architecture is present in Android: the floating navigation bar, More hub, editor toolbar and menu, right-side tool rail, appearance previews, draft Apply/Cancel behavior, markdown preview, voice-note entry point, themes, note cards, search, filtering, and persistence flows all have implementations.

The remaining work is primarily:

1. Confirming behavior and visual parity on a real phone at narrow, large-font, dark-theme, and keyboard-visible sizes.
2. Closing small prototype parity gaps such as the dashboard greeting/theme affordance and consistent empty/loading/error states.
3. Improving editor and dashboard performance before calling the redesign complete.
4. Adding end-to-end coverage for the interactions that the prototype makes central.

## Status Legend

- **Implemented**: Android owner exists and the behavior is represented in source/tests.
- **Partial**: The behavior exists, but differs from the prototype or lacks a required state/validation path.
- **Pending**: No reliable implementation or verification is present.

## Surface Comparison

| Prototype surface | Current Android state | Status | Owning files |
|---|---|---|---|
| Home dashboard with greeting, title, search, category chips and note grid | Dashboard, search, filters, sorting, grid/list and note cards exist. The prototype's time-based greeting and direct theme button are not present in the dashboard. | Partial | `NestNoteApp.kt`, `NoteCard.kt`, `NestNoteTheme.kt` |
| Floating Home / Notes / New / Reminders / More bar | All five destinations exist; More sub-destinations route back through More. Android uses Material icons and labels instead of prototype glyphs. | Implemented, device validation pending | `NestNoteNavBar.kt`, `NestNoteApp.kt`, `NestNoteE2ETest.kt` |
| More hub with Library, Data and Preferences groups | Groups and rows exist. Backup & restore and Settings intentionally share the Settings destination, which is functional but less direct than the prototype's separate row destinations. | Partial | `MoreScreen.kt`, `NestNoteApp.kt`, `SettingsScreen.kt` |
| Editor opening as a full writing surface | Shared create/edit editor exists with configurable full-screen, sheet and dialog modes. Prototype uses one animated surface; mode-specific Android behavior needs device review. | Partial | `NestNoteApp.kt`, `NoteEditor.kt`, `AppPreferences.kt` |
| Editor toolbar: Back, Colour, Format, More, Save | All actions exist with the same responsibilities. | Implemented, device validation pending | `NoteEditor.kt`, `EditorToolRail.kt` |
| Right-side Colour / Background / Format rail | Slim animated rail, previews, selected state, internal scrolling, Apply/Cancel and Back/outside-tap behavior exist. | Implemented, device validation pending | `EditorToolRail.kt`, `AppearancePanel.kt`, `NestNoteTheme.kt` |
| Draft appearance preview and cancellation | Colour/background changes remain draft until Apply; cancel and system Back revert them. | Implemented, regression coverage should be expanded | `NoteEditor.kt`, `EditorToolRail.kt`, `NestNoteE2ETest.kt` |
| Voice note panel with attach, record, pause/resume and stop | Android voice controls are hidden until More or existing recordings and use the production recording/storage classes. | Implemented, device/audio validation pending | `NoteEditor.kt`, `VoiceAudioController.kt`, `VoiceNoteAudio.kt`, `VoiceAttachmentStore.kt` |
| Markdown preview toggle | Android preview uses a safe parser and `AndroidView`; prototype uses an inline preview pane. | Implemented, performance validation pending | `NoteEditor.kt`, `MarkdownPreview.kt`, `MarkdownPreviewTest.kt` |
| Editor More menu: pin, reminder, voice, read mode, archive, share, copy, delete | All listed actions exist, including unsaved-change and delete confirmation flows. | Implemented, edge-case validation pending | `NoteEditor.kt`, `EditorModeViewModel.kt`, `NestNoteApp.kt` |
| Note cards and masonry-like dashboard | Android uses lazy grid/list layouts rather than CSS columns. This is an intentional native adaptation, not missing functionality. | Implemented, visual parity pending | `NoteCard.kt`, `NestNoteApp.kt` |
| Light/dark theme toggle | Theme modes and AMOLED support exist in Settings; the prototype exposes a direct dashboard toggle. | Partial | `NestNoteApp.kt`, `SettingsScreen.kt`, `AppPreferences.kt`, `NestNoteTheme.kt` |
| Reduced-motion behavior | Compose transitions are used throughout, but no explicit reduced-motion policy is visible in the redesign modules. | Pending | `NestNoteTheme.kt`, `NestNoteNavBar.kt`, `EditorToolRail.kt`, `NoteEditor.kt` |

## Incomplete and Pending Work

### P0 - Validate the core editor flow

- [ ] Run the prototype journeys on a physical Android 13 device: create note, edit note, open keyboard, type, preview Markdown, open/close rail, change colour/background, cancel, apply, save, reopen, and discard.
  - Files: `app/src/androidTest/java/com/nestnote/app/NestNoteE2ETest.kt`; add a focused editor regression test if the current E2E class becomes too broad.
  - Acceptance: no crash, no clipped toolbar or body, keyboard does not hide Save/Back, and cancelled appearance changes are not persisted.
- [ ] Validate the negative-height fix during repeated rail/voice transitions and keyboard resize.
  - Files: `app/src/main/java/com/nestnote/app/NoteEditor.kt`; `app/src/test/java/com/nestnote/app/NoteEditorTest.kt`.
  - Acceptance: no `Size(... x -N)` exception across repeated open/close and IME transitions.
- [ ] Add automated coverage for rail Apply, Cancel, outside tap, system Back, and reopening the editor with the saved appearance.
  - Files: `app/src/androidTest/java/com/nestnote/app/NestNoteE2ETest.kt`; optionally add `EditorToolRailRegressionTest.kt`.

### P1 - Close visible prototype parity gaps

- [ ] Decide whether the dashboard should expose the prototype's greeting and direct theme toggle. If approved, add the greeting and route the toggle through the existing theme preference state rather than creating a second persistence path.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteApp.kt`, `app/src/main/java/com/nestnote/app/NestNoteTheme.kt`, `app/src/main/java/com/nestnote/app/AppPreferences.kt`, `app/src/androidTest/java/com/nestnote/app/NestNoteE2ETest.kt`.
- [ ] Make the More hub's Backup & restore row open the backup section directly, while Settings opens the general settings surface.
  - Files: `app/src/main/java/com/nestnote/app/MoreScreen.kt`, `app/src/main/java/com/nestnote/app/NestNoteApp.kt`, `app/src/main/java/com/nestnote/app/SettingsScreen.kt`.
  - Acceptance: Back returns to More and the selected destination is clear.
- [ ] Audit empty, loading, recovery, disabled and success states against the prototype's calm visual language. In particular verify empty Archived, Pinned, Reminders, Categories, no-search-results, storage recovery, and save-failure states.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteApp.kt`, `app/src/main/java/com/nestnote/app/MoreScreen.kt`, `app/src/main/java/com/nestnote/app/SettingsScreen.kt`, `app/src/androidTest/java/com/nestnote/app/NestNoteE2ETest.kt`.
- [ ] Audit the Android card layout against the prototype at phone width, landscape width, large text, and list mode. Keep lazy layouts; do not reproduce CSS masonry if it harms accessibility or stable measurement.
  - Files: `app/src/main/java/com/nestnote/app/NoteCard.kt`, `app/src/main/java/com/nestnote/app/NestNoteApp.kt`.

### P1 - Performance and recomposition follow-up

- [ ] Move initial `NoteStore.loadSafely()` off the first composition path or otherwise measure and bound its startup cost.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteApp.kt`, `app/src/main/java/com/nestnote/app/NoteStore.kt`.
  - Acceptance: system trace shows no long storage/crypto/JSON task blocking the first frames.
- [ ] Derive filtered/sorted notes only when their inputs change, and avoid recalculating counts unrelated to the current destination.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteApp.kt`; add pure helper tests if extraction is made.
- [ ] Avoid reparsing the entire Markdown body during every `AndroidView` update when preview content has not changed; isolate or cache preview conversion.
  - Files: `app/src/main/java/com/nestnote/app/NoteEditor.kt`, `app/src/main/java/com/nestnote/app/MarkdownPreview.kt`, `app/src/test/java/com/nestnote/app/MarkdownPreviewTest.kt`.
- [ ] Measure checklist parsing and word-count work during continuous typing before optimizing. If traces show cost, derive them from the body text in a narrower recomposition scope or cache the results.
  - Files: `app/src/main/java/com/nestnote/app/NoteEditor.kt`, `app/src/main/java/com/nestnote/app/NoteText.kt`, `app/src/test/java/com/nestnote/app/NoteTextTest.kt`.
- [ ] Capture a cold-start and note-editing system trace and record findings in `WORKLOG.md`. No performance claim should be marked complete without a device trace.

### P1 - Accessibility and responsive validation

- [ ] Check TalkBack labels, selected semantics, focus order, 48dp touch targets, contrast for every colour/background combination, and large font scaling.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteNavBar.kt`, `app/src/main/java/com/nestnote/app/EditorToolRail.kt`, `app/src/main/java/com/nestnote/app/NoteEditor.kt`, `app/src/main/java/com/nestnote/app/NoteCard.kt`.
- [ ] Check IME/insets and edge-to-edge behavior on Android 13, including editor sheet/dialog modes and the right rail while the keyboard is visible.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteApp.kt`, `app/src/main/java/com/nestnote/app/NoteEditor.kt`, `app/src/main/java/com/nestnote/app/EditorToolRail.kt`.
- [ ] Respect reduced-motion preferences for navigation, editor, rail, and collapsible voice transitions.
  - Files: `app/src/main/java/com/nestnote/app/NestNoteTheme.kt`, `app/src/main/java/com/nestnote/app/NestNoteNavBar.kt`, `app/src/main/java/com/nestnote/app/EditorToolRail.kt`, `app/src/main/java/com/nestnote/app/NoteEditor.kt`.

### P2 - Documentation and release closure

- [ ] Reconcile this audit with `ROADMAP.md`, `UI_UX_RULES.md`, and `WORKLOG.md` after each completed item.
- [ ] Keep the supplied prototype file as a reference artifact; do not treat it as production code.
- [ ] Run unit tests, debug build, instrumentation compilation, and device smoke tests on the final commit; record exact commands and results in `WORKLOG.md`.

## Suggested Implementation Order

1. Device validation of editor, keyboard/insets, rail dismissal, and appearance persistence.
2. Add focused rail/editor regression tests.
3. Profile startup and note editing; address only measured jank.
4. Resolve direct theme/greeting and More routing parity decisions.
5. Complete accessibility, large-font, reduced-motion, and responsive checks.
6. Update roadmap/worklog and perform final release checks.

## Explicitly Not Required for Prototype Parity

- Replacing Compose lazy grids with CSS-style masonry columns.
- Copying prototype emoji/glyph icons into the Android app; Material icons with equivalent semantics are preferable.
- Adding browser-only prototype behavior such as placeholder Reminders/Notes tabs; Android already has functional destinations.
- Replacing the existing encrypted local storage format.
