# NESTNOTE Work Log

Last updated: 2026-10-12

> Maintenance rule: update this log in the same commit as every implementation commit, including the commit SHA, summary, and validation status. If a separate commit is unavoidable, update the log immediately afterward.  
Repository: [dukeoftoad/toadNOTES](https://github.com/dukeoftoad/toadNOTES)

This activity record is assembled from visible repository commits, pull requests, and GitHub Actions runs. Statuses reflect the latest checks observed when written; use the linked run for live status.

## Current snapshot
- Native Android app using Kotlin, Jetpack Compose, and Material 3; target SDK API 36, minimum SDK 26.
- `main` includes merged PRs #1–#5 as of 2026-10-12. Verify live PR status before relying on this snapshot.
- PR #6, `security/privacy-hardening`, is intentionally on hold and remains unmerged pending owner approval and device/release validation.
- PR #7, `feature/phase-6-release-checklist`, is open against `main` and now includes Phase 6 reliability work; it is not merged.
- Phase 4 (PR #4) and Phase 5 (PR #5) were merged on 2026-10-12; older entries below describing them as open/in progress are historical snapshots, not current status.
- Note persistence uses SharedPreferences / NoteStore. Phase 6 is incrementally hardening local-save behavior and backup/restore before larger architecture changes.
- Emulator UI workflow is manual-only by project decision. Do not claim device testing unless it was actually run.

## Pull requests
| PR | Title | Status | Scope |
|---|---|---|---|
| [#1](https://github.com/dukeoftoad/toadNOTES/pull/1) | feat: Material 3 personalization and flexible note creation | **Merged** 2026-10-11 | Phase 1 dashboard modernization and personalization. |
| [#2](https://github.com/dukeoftoad/toadNOTES/pull/2) | feat: resilient inline note editor | **Merged** 2026-10-11 | Unified create/edit experience and draft safety. |
| [#3](https://github.com/dukeoftoad/toadNOTES/pull/3) | Expand settings | **Merged** 2026-10-11 | Theme, colors, layout, sorting, and note display preferences. |
| [#4](https://github.com/dukeoftoad/toadNOTES/pull/4) | feat: modernize everyday note-taking experience | **Open draft — awaiting owner review** | Phase 4 editor refinement, sorting, timestamp compatibility, archive/restore, deletion Undo, and explicit reminder-status messaging. |

## Work completed
### Foundation and delivery automation
- Created the native Kotlin / Jetpack Compose Android app.
- Added Android CI/CD and unit/instrumentation-test scaffolding.
- Fixed SDK setup and Compose UI-test dependency issues in CI.
- Added automated APK prereleases from main and corrected the release artifact path.
- Kept required CI focused on API 36; emulator UI workflow was made manually triggered after emulator reliability issues.

### Phase 1 — home screen (merged)
- Modernized the dashboard toward a calm, compact note-taking layout.
- Added compact/comfortable density preference and adaptive overview.
- Added note sort options and soft pastel / wallpaper-adaptive note colors.
- Improved category-chip styling, note-card color rendering in list layout, and create-note action.
- Added tests for dashboard/search/sorting, categories/empty state, note-color choices, settings/layout preferences, and related interactions.
- PR: [#1](https://github.com/dukeoftoad/toadNOTES/pull/1).

### Phase 2 — editor resilience (in progress)
Branch: `feature/phase-2-note-editor`; PR: [#2](https://github.com/dukeoftoad/toadNOTES/pull/2).
- Changed title, body, category, color, and reminder fields to Compose saveable state keyed to note ID.
- Added discard confirmation for explicit editor close and Cancel when meaningful unsaved changes exist.
- Added an instrumentation test for discard confirmation.
- Preserved NoteStore schema and local persistence format.
- Remaining: restore editor visibility/selection after activity recreation; route system/back and sheet dismissal through the same unsaved-change guard; review create/edit consistency and finish CI validation.

## Validation and CI records
- [Android CI — Phase 1 merged head](https://github.com/dukeoftoad/toadNOTES/actions/runs/38129704278): success.
- [Android Release — Phase 1 merged head](https://github.com/dukeoftoad/toadNOTES/actions/runs/38129704259): success.
- [Android CI — earlier Phase 1 commit](https://github.com/dukeoftoad/toadNOTES/actions/runs/38129329854): success.
- [Android CI — earlier Phase 2 commit](https://github.com/dukeoftoad/toadNOTES/actions/runs/38129805061): cancelled after a newer commit was pushed.
- [Android CI — current Phase 2 head f27e190](https://github.com/dukeoftoad/toadNOTES/actions/runs/38129835034): **in progress** when written. Do not record it as passed until complete.
- [Android UI Tests (API 36)](https://github.com/dukeoftoad/toadNOTES/actions/runs/38129320213): separate/manual workflow; not a required CI gate under the current project decision.

## Commit history
Visible commit history, newest first (short SHA, UTC date, subject; merge commits included).
| Commit | Date (UTC) | Subject |
|---|---|---|
| `a7532b3` | 2026-10-11 | Merge PR #1 — Material 3 personalization and flexible note creation |
| `1d3c600` | 2026-10-11 | test: remove weak dashboard assertion |
| `1f2a5dd` | 2026-10-11 | ci: keep emulator UI tests manually triggered |
| `af7f825` | 2026-10-11 | test: cover dashboard categories and empty state |
| `d85a7b5` | 2026-10-11 | ci: run API 36 UI tests on pull requests |
| `994230e` | 2026-10-11 | fix: pass selected note color style in list layout |
| `8faefe7` | 2026-10-11 | test: cover selectable adaptive and pastel note colors |
| `54b4615` | 2026-10-11 | ui: use prominent round create-note action |
| `823b27d` | 2026-10-11 | feat: add selectable pastel and wallpaper-adaptive note colors |
| `8861a7e` | 2026-10-11 | test: cover reference home search and sorting |
| `763cd46` | 2026-10-11 | feat: add working note sort options and soft note colors |
| `1767d6d` | 2026-10-11 | ui: match calm compact NestNote home design |
| `bcc3a83` | 2026-10-11 | test: scroll through settings in UI regression coverage |
| `f22f464` | 2026-10-11 | test: expose settings scroll container for UI automation |
| `6fc92d0` | 2026-10-11 | ci: compile Android UI tests in pull request validation |
| `37a7ac1` | 2026-10-11 | test: cover modern home overview and saved layout preference |
| `fc29813` | 2026-10-11 | ui: modernize home with adaptive overview and note section |
| `2c02926` | 2026-10-11 | ui: align note category chips with Material You colors |
| `61b32e8` | 2026-10-11 | fix: remove duplicate dashboard density state |
| `a449cea` | 2026-10-11 | feat: add compact and comfortable dashboard density |
| `c1aca0c` | 2026-10-11 | feat: add compact and comfortable dashboard density |
| `b73b2ff` | 2026-10-11 | test: target the accessible search action |
| `bfcdf05` | 2026-10-11 | test: cover editor preferences and Material 3 note creation |
| `fa204d0` | 2026-10-11 | feat: add Material 3 personalization and note editor options |
| `91f712a` | 2026-10-11 | Add manually triggered API 36 UI test workflow |
| `c3d0104` | 2026-10-11 | Keep required CI API 36-only and independent of emulator boot |
| `5055efa` | 2026-10-11 | Stabilize Android UI test emulator boot |
| `8e90fe4` | 2026-10-11 | Fix Compose UI test assertion API |
| `e0a02dd` | 2026-10-11 | Fix automatic release APK artifact path |
| `afcc172` | 2026-10-11 | Enable automatic APK prereleases from main |
| `d55e566` | 2026-10-11 | Fix Compose UI test dependency version resolution |
| `f50c31f` | 2026-10-11 | Fix Android release SDK setup by dropping obsolete tools package |
| `ac4a8ea` | 2026-10-11 | Fix Android CI SDK setup by dropping obsolete tools package |
| `03fee55` | 2026-10-11 | Add temporary CI APK signing for validation |
| `34e4fa3` | 2026-10-11 | Add Android CI/CD and unit/E2E tests |
| `d4a5f26` | 2026-10-11 | Add native Android 16 app with Kotlin and Jetpack Compose |
| `129b668` | 2026-10-11 | Initial NESTNOTE application commit |

## Recent Phase 2 commits
| `be7da73` | Refined the launcher vector to include its own rounded-square deep-teal background, avoiding a full-bleed square when used as a legacy launcher drawable. The generated reference image was used as visual direction; vector remains a hand-built approximation, not a pixel-exact embedded PNG. | CI pending. |

| `9c1b6b2` | Added a custom NESTNOTE launcher vector drawable inspired by the generated icon concept (deep teal tile, layered pastel notes, folded golden corner, writing lines, N mark, pencil). | Pending CI. |
| `dada8ae` | Applied `@drawable/nestnote_launcher` as the Android application icon in the manifest. | Pending CI. |

| `ca8a723` | Fixed note-card foreground contrast: pastel cards now use stable dark primary text, secondary text, and icons rather than wallpaper-derived yellow/bright theme colors; adaptive cards still use Material 3 foreground colors. | CI pending; verify build before merge. |
| Commit | Change | Validation |
|---|---|---|
| `e6db24a` | Save editor visibility and selected note ID across activity recreation; updated release workflow to use a rolling `latest` prerelease titled `NESTNOTE Latest Build` in the same branch commit `c47242e`. | Android CI failed at Kotlin compile on `MainActivity.kt:338` due to one remaining assignment to the now-derived `editorNote`; corrected in follow-up commit (see next row). |
| `124873a` | Fixed remaining note-card open handler to update `editorNoteId`. | CI failed due to conflicting/mistyped derived editor-note declaration; fixed in the following commit. |
| `def342a` | Corrected derived selected-note lookup to `notes.firstOrNull { it.id == editorNoteId }`. | CI re-run pending; current head is not yet validated. |

| `86469e6` | Added a parent-level unsaved-draft guard for system/back and sheet/dialog dismissal requests, while keeping editor Save/confirmed Cancel paths able to close directly. | CI pending; behavior needs automated validation. |

## Maintenance
Update this file when a PR merges, an implementation stage completes, or a material CI decision/status changes. Record factual outcomes only; distinguish planned, in-progress, and verified work.


## Phase 3 — Settings expansion

| Commit | Work | Validation |
|---|---|---|
| `63fb556` | Added persistent preferences for default sort order, note preview visibility, and date/reminder visibility. | CI pending. |
| `360535f` | Added the Note behavior group to Settings with controls for sorting, previews, and timestamps/reminders. | CI pending. |
| `a2e9b0a` | Connected note-card rendering to the preview and timestamp preferences. | CI pending. |

## Validation policy

- Normal Android CI is required for each meaningful increment.
- Emulator UI workflow remains manual-only; visual verification must be recorded as manual when performed.
- Do not mark Phase 3 complete until preference persistence and existing note/editor flows are verified.
- Preserve the existing NoteStore format and user notes.

## Next steps

1. Run Android CI and fix any compile/test failures.
2. Add tests for preference persistence and preview/timestamp toggles.
3. Add default note category and starting category behavior.
4. Expand settings into Appearance, Layout, Note behavior, and Data & privacy groups without rewriting note storage.

| `dc9d246` | Fixed the list-layout NoteCard call so preview and timestamp preferences apply consistently in both grid and list modes. | CI pending; caught during code review before CI. |


## Phase 3 plan refinement — 2026-10-11

- Reviewed Android Developers Settings design guidance and Google Keep's note settings patterns before expanding the feature set.
- Plan: audit and stabilize existing controls; then Appearance, Layout, Note behavior, and persistence/testing in small validated increments.
- Keep settings grouped and scannable; every control must affect the UI and persist. Preserve NoteStore and only consider DataStore after auditing existing preference storage and migration safety.
- `dc9d246`: fixed preview/timestamp settings propagation in list layout. `b1dfcb6`: updated this worklog. CI run `38135347194` is in progress for the latest branch head at time of check.


### Follow-up implementation — 2026-10-11

| Commit | Work | Validation |
|---|---|---|
| `3db8c07` | Made the dashboard sort control label reflect the active sort mode (Recently edited vs Title A–Z), so the visible selection stays in sync with the persistent sort preference and menu selection. | Code change committed; CI intentionally not polled per user instruction. |

| `a0431be` | Added a persistent default note category setting and applied it to newly created notes in the primary editor; existing notes retain their saved category. | Code change committed; CI intentionally not polled per user instruction. |

| `838277e` | Fixed the Quick dialog editor to honor the saved default note category for new notes, matching the sheet and full-screen editor behavior; editing an existing note still preserves its category. | Code change committed; checks not polled per user instruction. |

| `39b87ca` | Fixed dashboard sort menu persistence: selecting Recently edited or Title A–Z now saves the preference immediately, matching Settings and surviving app recreation. | Source-level review; Android CI for this exact commit not yet observed. |

### Phase 3 final review — 2026-10-11


| `39b87ca` | Dashboard sort menu now writes its selection to preferences immediately. | Source reviewed; CI for this change pending. |
| `806ab2e` | Clarified Settings grouping with Layout, Layout density, Note editor, and Data & privacy; privacy copy explicitly states cloud sync and built-in export/import are not currently provided. | Source reviewed; CI for this change pending. |
| `8c3cba0` | Extended the dashboard sorting UI test to assert the selected sort mode is persisted. | Test added; UI workflow remains manual-only by project decision. |


## Phase 4 — Everyday note-taking (in progress)

- PR #3 (Settings expansion) was merged into `main`; Phase 4 work starts from the updated `main` on `feature/phase-4-note-taking`.
- `075db52`: first UI modernization increment for the note editor. Replaced the decorative text close mark with an accessible Material icon, updated title/body fields to use rounded Material 3 tonal surfaces and clearer writing placeholders, enlarged the body writing area, and made selected note colors more visible with an outlined selection state.
- Validation: Android CI run [#38136566682](https://github.com/dukeoftoad/toadNOTES/actions/runs/38136566682) passed for the PR head `71737e1`. Emulator UI workflow remains manual-only. No emulator or physical-device validation is claimed.
- Next increments: review remaining editor actions and reminder affordances; then tackle sorting/created timestamps, note colors/categories, archive/undo, and reminder reliability in separate tested steps.


### Phase 4 visual direction

The following reference images supplied for this project are inspiration only; they are not NESTNOTE screenshots or assets. Implementation should translate the visual direction into native Jetpack Compose / Material 3 patterns rather than copying another app.

1. [Android note-taking app inspiration — modern note cards and dashboard](https://images.openai.com/static-rsc-4/zPyVdHCYj04ct21srgrGTcWu_17kCukCcLmEEGjEC8BnOsuYqI_9G4maUXl6Ia60eRonulOwA0uukYCIjW1zGTLG-SG-LL7xMGMBZzBaxwWiVzG2JTS3EPwl2aifome1AsdR_eFxjErd0ipI77gsfueLC6m8TlUgjoydJK0WcZPTz6LeQw6Rv5c7cHCpnF9r?purpose=fullsize)
2. [Minimal note-taking UI inspiration — clean hierarchy and writing space](https://images.openai.com/static-rsc-4/toLmSKmVIwCJpy33vaFcGMEMLl5PfFobOvb7V5sE8rzQO68T8pqatmExHE64MiczCHrfO5OFwQ7pEqxsU5IdhSLC9jmOKX1wgu6X_D_UuH4iMTS_M_ZLw8qF45PI_Z9k-9ZePKbItX9huFMmRjuwyIRsNTTqIBxDCQj_71TxGObzBJf70d-UrjIMYjoYX3ug?purpose=fullsize)
3. [Google Keep formatting inspiration — readable typography and note controls](https://images.openai.com/static-rsc-4/Hfu_zLkOAJMCUEC24TajWwplqoK-sqrfB-Z2eP9_2CBy373ZUWP_9r-lxGYe8b-HcQiPBTZkGufQohXuOxRYIH5vbD9xbk03NXvZHZyKnYyW08wEA9OQw2m5WE5SwE3yv5erK7sETVFQH5Z4vmS3JhXlhCCWUUMTSGWgt5CWnpn-vgSLCgzINJUAIYxuuEx3?purpose=fullsize)

Design principles:
- Current Android / Material 3 feel, never legacy Android 5 styling.
- Calm compact dashboard, generous readable editor canvas, rounded tonal fields and restrained elevation.
- Clear title/body hierarchy, comfortable keyboard editing, accessible icon labels and touch targets.
- Light/dark theme parity, dynamic color support, readable pastel-card contrast, and responsive insets.
- Preserve existing note data and editor behavior while modernizing; visual polish must not regress save, cancel, or draft restoration.


### Phase 4 implementation increment — created-date sorting

- `7308052`: Added a backward-compatible `created` timestamp to the existing JSON note record. Older records without the field fall back to their saved ID; editing a note preserves its creation timestamp.
- `b77c543`: Added Recently created sorting while retaining pinned notes at the top and preserving Recently edited / Title A–Z. The chosen sort mode persists using the existing preferences key.
- `b677784`: Extended the sorting UI test to check that Recently created is available and persists as `CREATED`.
- Validation: Android CI passed for the earlier editor UI increment at run [#38136566682](https://github.com/dukeoftoad/toadNOTES/actions/runs/38136566682). The current changes have triggered a new CI run; do not mark them verified until that run finishes. Emulator UI workflow remains manual-only.


### Phase 4 implementation increment — creation timestamp regression coverage

- Added unit tests proving older saved JSON without a `created` field loads with a deterministic ID-based fallback, and that the original creation timestamp remains stable when an edited note is saved with a newer `updated` value.
- Validation: Android CI run [#38137019018](https://github.com/dukeoftoad/toadNOTES/actions/runs/38137019018) passed for the prior Phase 4 head. The new regression tests are committed here and require a fresh CI result. Emulator UI workflow remains manual-only; no device testing is claimed.


## Phase 4 current progress
Branch: `feature/phase-4-note-taking`; draft PR: [#4](https://github.com/dukeoftoad/toadNOTES/pull/4). Reference images are recorded in the PR description and this branch's earlier Phase 4 entries as inspiration, not app screenshots.

- Modernized the editor's Material 3 surfaces, title/body hierarchy, color swatches, and close affordance.
- Added persisted creation timestamps and Recently created sorting, preserving pinned-first ordering and falling back safely for older stored notes.
- Added unit regression tests for legacy JSON without `created` and for preserving creation time while edit time advances.
- Latest confirmed Android CI: [run #38137412765](https://github.com/dukeoftoad/toadNOTES/actions/runs/38137412765) — **success** on commit `90081fe`.
- Next: undo deletion, make reminder status explicit (without implying notifications are scheduled), then continue pin/archive behavior and test coverage. Emulator UI workflow remains manual-only; no physical-device testing is claimed.

### Commit `90081fe` — test: cover legacy and stable note creation timestamps
- Added two NoteStore regression tests and updated the roadmap/worklog status record in the follow-up documentation commit.
- Validation: [Android CI run #38137412765](https://github.com/dukeoftoad/toadNOTES/actions/runs/38137412765) succeeded for this code head.


### Phase 4 increment — undo deletion and reminder clarity

- Added a Material 3 snackbar with an Undo action to note deletion from dashboard cards and the editor. Undo restores the deleted note to the persisted collection and retains its original metadata.
- Added explicit reminder copy clarifying that the selected date/time is saved as note metadata and Android notifications are not enabled yet.
- Initial CI failure was a Kotlin compile error caused by removing the old `toastMessage` state while leaving its save-toast references. Reused the new shared snackbar host for save feedback and renamed the editor delete callback to avoid shadowing the shared delete helper.
- Validation: CI run [#38138296608](https://github.com/dukeoftoad/toadNOTES/actions/runs/38138296608) found a remaining function-reference mismatch in the full-screen editor (`onDelete = deleteNote`). Follow-up fix uses the same guarded `editorDeleteAction` callback as the sheet editor. Re-run CI before treating this increment as verified. Emulator UI workflow remains manual-only; no physical-device testing is claimed.


### Phase 4 increment — persistent archive state

- Added a backward-compatible `archived` field to the existing note JSON format; legacy records default to active.
- Added an Archived navigation destination and an Archive action on note cards. Archiving unpins the note and removes it from active home/category/reminder views without deleting its content.
- Added **Restore from archive** directly in archived note actions. Restore returns the note to active views; it remains unpinned, and the updated timestamp reflects the change.
- Expanded the store regression test to verify both archive and restore states survive save/load.
- Fixed the list-layout delete action to use the shared Undo snackbar helper, matching grid and editor deletion.
- Added NoteStore tests for archived-state round-trip and legacy default behavior.
- Validation: prior editor callback fix passed Android CI run [#38138686605](https://github.com/dukeoftoad/toadNOTES/actions/runs/38138686605). This archive increment needs its own Android CI run; emulator UI tests remain manual-only.


## Phase 4 cross-check — 2026-10-11

Reviewed the latest implementation source, Phase 4 roadmap, draft PR description, supplied visual-reference links, and Android CI result.

- Latest implementation head: `63951027023f343f85be8fa35a835713d82629dc` — grid/list archive and restore callbacks now use the same state transition; restore persistence and an archived-screen restore instrumentation test were added.
- [Android CI run #38139703593](https://github.com/dukeoftoad/toadNOTES/actions/runs/38139703593): **success**. This is the required automated CI result; it does not mean the emulator UI test was run.
- UI/design review: editor uses Material 3 tonal surfaces, clearer title/body hierarchy, a larger writing area, explicit close icon, improved color swatches, and save feedback. Dashboard uses adaptive grid/list layouts, pastel/adaptive note colors, sorting, pinning, archive access, and an Undo snackbar. The supplied images are design inspiration, not app screenshots; exact pixel parity has not been verified.
- Scope clarification: reminder time is stored/displayed as metadata only; no alarm or notification delivery is implemented. The third visual reference suggests text formatting, but this branch does not add a rich-text formatting toolbar. These are not claimed as implemented.
- Validation boundary: instrumentation tests are included/compiled by the Android CI workflow, but the separate emulator UI workflow remains manual-only. No emulator or physical-device visual verification is claimed.
- Documentation and PR description were reconciled with the implemented scope. Phase 4 remains **in progress / awaiting owner approval**; do not mark complete or merge PR #4 until explicit approval.


### Focused UI pass — editor redesign and appearance picker — 2026-10-11

Scope was intentionally narrowed to the note editor and its appearance picker; dashboard redesign was not included.

- `b8dec6f`: redesigned the editor around a spacious full-screen title/body canvas, back/pin/reminder/archive/palette actions, low-chrome text fields, category selection, and a persistent save/cancel footer. Fresh installs now default to the full-screen editor; an explicit saved editor-mode preference continues to be honored.
- `cd9914b`: added a backward-compatible `background` index to the existing note JSON. Existing notes default to no custom wallpaper; color and wallpaper choices save with note content and survive reloads.
- Added a native Compose appearance bottom sheet with a large preview, seven pastel colour swatches, five illustrated wallpaper choices plus “none”, selected-state indicators, and a Done action. The illustrations are drawn locally in Compose; no remote image assets or new dependencies are required.
- `4ae78f4`, `7bab9ee`, `12e2e1e`, `0822ab6`, and `4760254`: exposed picker choices to UI tests, covered wallpaper persistence and legacy defaults in NoteStore unit tests, updated editor interactions in instrumentation coverage, and added a picker dismissal action.
- The existing grid/list home screen, note sorting, archive/restore, and deletion undo were not redesigned. Notes edited through the quick-dialog mode preserve their saved wallpaper value.
- Validation status at implementation time: **Android CI pending for the latest branch head**. The emulator UI workflow remains manual-only; no emulator or physical-device visual run is claimed. PR #4 remains open/draft and must not be merged without owner approval.


### Validation follow-up — editor and appearance picker — 2026-10-11

- `f8bebeb`: made the chosen note color visible in the editor canvas and as a tinted overlay beneath selected wallpaper art; note cards use the saved color tint with wallpaper art as well.
- Android CI [run #38146442164](https://github.com/dukeoftoad/toadNOTES/actions/runs/38146442164): **success** on implementation commit `f8bebeb`. Unit tests, debug APK build, and instrumentation-test compilation all passed.
- This CI run compiles the UI tests; it does not execute the manual-only emulator UI workflow. No device screenshot comparison is claimed.
- Branch remains `feature/phase-4-note-taking`; PR #4 remains open and in draft pending owner approval.


### Palette contrast refinement — 2026-10-11

- `a2485f5`: adjusted the editor/appearance-picker palette to use more distinguishable rose, peach, yellow, sage, teal, and blue swatches while keeping the warm neutral option. Matching note tints remain light enough for dark body text.
- Increased the selected/unselected swatch border contrast and unified the selection indicator to a deeper purple; shifted the picker surface closer to a clean warm-white.
- Validation: CI status for this palette-only commit is pending. No emulator/device visual test claimed.


### Device screenshot contrast fix — 2026-10-12

- User-provided device screenshot showed nearly invisible toolbar icons, yellow-on-cream body text/cursor, and a disabled Save button with insufficient contrast.
- `fb88176`: explicitly set editor text to dark graphite, placeholders to readable mid-grey, cursor and action accents to deep purple, toolbar icons to graphite/purple, and Cancel/Save button colors including a readable disabled state. Increased colour-swatch saturation while keeping the note background tints soft.
- Automated CI status is pending for this commit. Device screenshot should be rechecked after installing the build; no post-fix emulator/device verification is claimed.


### Contrast fix validation — 2026-10-12

- Owner confirmed all checks are passing after `fb88176`, the editor contrast fix prompted by the supplied device screenshot.
- Android CI [run #38193073138 — success](https://github.com/dukeoftoad/toadNOTES/actions/runs/38193073138) completed successfully for the current PR head `a60885c` (including the follow-up worklog commit).
- The fix makes editor text graphite, placeholders mid-grey, toolbar/action icons graphite or deep purple, the cursor purple, and Save/Cancel actions more legible; pastel swatches were strengthened without making the writing surface harsh.
- Remaining manual step: owner should confirm the newly installed build on-device visually. No post-fix screenshot/device verification is claimed by the implementation agent.
- PR #4 remains open and draft, awaiting explicit owner approval; Phase 4 is not marked complete and nothing is merged.


### Phase 4 finalisation — 2026-10-12

- Owner confirmed validation/testing passed and approved PR #4 as good to go.
- PR #4 was marked ready for review and squash-merged into `main`: [PR #4](https://github.com/dukeoftoad/toadNOTES/pull/4), merge commit `b3a140efcded6fc2fe090be0a6b997c9c83b6bcc`.
- Android CI was confirmed passing by the owner; recorded run: [#38193073138](https://github.com/dukeoftoad/toadNOTES/actions/runs/38193073138).
- Phase 4 is now marked complete in `ROADMAP.md`. The note editor redesign, appearance picker, persisted wallpaper/color choices, and screenshot-driven contrast/palette refinements are included.
- Scope caveats remain accurate: reminder times are metadata only; rich-text formatting is not implemented. Owner performed the validation; no separate emulator screenshot run is claimed by the implementation agent.
- PR #4 is merged. No further merge action is pending.


## Phase 5 increment — Settings and editor UX — 2026-10-12

Branch: `feature/phase-5-settings-editor-ux` (from `main` after PR #4 merge). Not merged; needs owner review.

- **Settings:** rebuilt with grouped sections using segmented controls (theme, dashboard view, density, sort, editor text size), selectable cards (note card colors, editor mode), default-category chips, switch rows with summaries, and a confirmed reset-to-defaults. Sorting can now be chosen as Edited / Created / Title from Settings (previously only a title toggle).
- **Editor:** checklist (☐/☑) and bullet toolbar; pressing Enter continues a list item and ends the list on an empty item; optional live word count, character count and reading time; editor text size (Small/Medium/Large); category chips instead of a dropdown; reminder presets with a clear button; Share and Copy text from the overflow menu; new notes open with the title focused; edited/created info for existing notes.
- **Backup/restore:** `NoteStore.encode/decode/merge` (same JSON format as local storage). Settings → Back up notes writes a file chosen via the system file picker; Restore backup merges by note id (new ids added, newer `updated` wins, nothing deleted) and rejects files that are not NESTNOTE backups.
- **Tests added:** `NoteTextTest` (list, checklist, counters, reminder presets), `NoteBackupTest` (round trip, validation, merge), and two instrumentation tests (settings text size/backup controls, checklist button).
- **Validation at implementation time:** the pure logic in `NoteText.kt` was compiled with a standalone Kotlin compiler and its cases passed. The Compose UI and Robolectric/instrumentation suites were not built locally because no Android SDK was available.
- **Android CI:** [run #38196257185 — success](https://github.com/dukeoftoad/toadNOTES/actions/runs/38196257185) on implementation commit `37055c10c5064129517ddd2efd82e4fc0928a921`, and [follow-up run #38196444610 — success](https://github.com/dukeoftoad/toadNOTES/actions/runs/38196444610) on branch head `7e9134a295ae93c41bb7e8c7a6412ff7ca89a9e4`. Both runs passed unit tests, the API 36 debug APK build, Android instrumentation-test compilation, and test-APK signing. Instrumentation tests were compiled, not run on a device; no emulator or physical-device testing is claimed.
- **Review status:** [PR #5](https://github.com/dukeoftoad/toadNOTES/pull/5) was opened as a draft and is ready to move to review after the final documentation commit's CI check. It targets `main`; no merge performed, awaiting owner review.
- **Known limits:** reminders are still metadata only; checkboxes toggle via the toolbar rather than by tapping; import accepts only NESTNOTE JSON backups.


### Phase 5 documentation reconciliation — 2026-10-12

- Refreshed `README.md` to describe the shipped Phase 5 feature scope, backup/restore merge semantics, project structure, build/test commands, and explicit limitations.
- Reconciled the Phase 5 status in `ROADMAP.md`: automated Android CI success is recorded separately from manual UI/accessibility/keyboard/edge-to-edge checks, which remain outstanding. Phase 5 is not marked complete.
- The implementation PR remains [#5](https://github.com/dukeoftoad/toadNOTES/pull/5), targeting `main`. The owner has requested discovery, documentation updates, and merge. Device/emulator instrumentation execution is not claimed.


### Phase 5 merge — 2026-10-12

- Owner requested discovery, documentation reconciliation, and merge of PR [#5](https://github.com/dukeoftoad/toadNOTES/pull/5). The PR was squash-merged into `main`; merge commit: `ee046eaa67fe2b2be713ef96becabf0892f034fe`.
- Final docs now include the README feature/limitations/build guide, roadmap status, and this worklog history. Phase 5 remains in progress until the manual UI, accessibility/larger-font, keyboard/edge-to-edge, and final regression checks are completed.
- CI runs recorded before the final documentation-only commits passed. No fresh workflow run was returned for the final docs head. Instrumentation tests were compiled, not executed on a device.


## Phase 6 — Reliability, Recovery & Accessibility (current work on PR #7)
**Status: P0 implementation in progress. Initial storage/backup increment passed CI; latest editor safeguards are awaiting a fresh CI result.** Branch: `feature/phase-6-release-checklist`; PR: [#7](https://github.com/dukeoftoad/toadNOTES/pull/7). No changes have been merged to `main`.

- [x] Validate note collections for duplicate/non-positive IDs, invalid timestamps, field length limits, and maximum record count.
- [x] Make local persistence report commit success/failure; keep the editor open when a note save fails.
- [x] Distinguish unreadable local storage from a fresh install. Corrupt stored bytes are left untouched; normal note writes are blocked until recovery.
- [x] Add versioned JSON backup envelopes (schema version 1) while retaining import compatibility for legacy JSON-array backups.
- [x] Validate a complete backup and show an add/update preview before applying restore; restore merges without deleting notes absent from the backup.
- [x] Add unit coverage for duplicate IDs, invalid/oversized data, corrupt local storage recovery, backup versioning, and legacy backup compatibility.
- [x] Android CI passed for the initial storage/backup increment (run #129, commit `c3155b2a92d16996f025bc8be09d762613e2ef2f`). Newer editor lifecycle/stale-draft commits still require CI validation.
- [x] Preserve the unsaved-change warning state across activity recreation and add a note-version guard that rejects stale editor drafts instead of overwriting newer saved content.
- [x] Interactive checklist controls are available in the editor; tapping a checkbox or its item text toggles the selected row while preserving other lines and indentation.
- [x] Completed checklist items are visually distinguished with muted color and strikethrough styling.
- [ ] Continue with large-font/small-screen accessibility checks and repository/ViewModel boundaries.
- [ ] Reconcile remaining historical work-log entries and update roadmap/checklist status after each verified milestone.

**Validation honesty:** CI run [#38220086471](https://github.com/dukeoftoad/toadNOTES/actions/runs/38220086471) passed for commit `35d5f86e8ff956ccec00ed30a0cf627620f11231`, including unit tests, API 36 debug build, instrumentation-test compilation, and test APK signing. The subsequent completed-item styling commit still requires CI validation. No emulator or physical-device validation is claimed.


**Latest Phase 6 update (2026-10-12):** Added stale-draft version checking for full-screen and quick-note editors. A draft carries the version timestamp it was opened from; the parent compares that value with the latest saved note before writing. A mismatch produces a recovery-oriented message and leaves the newer note untouched. The unsaved-change warning flag now survives activity recreation alongside the saveable editor draft. Unit coverage includes the version comparison helper. This change is not yet CI-validated.


### Phase 6 accessibility increment — 2026-10-12

- Android CI run [#142](https://github.com/dukeoftoad/toadNOTES/actions/runs/38212595857) passed on commit `59e33cc6dfff9694ac97c6c9575e6e5bfe871600`, validating the newline-fixture regression fix before the next change.
- Checklist progress on note cards now remains visible when note-body previews are turned off, so disabling previews no longer hides a useful task-status summary.
- The progress label uses body-small typography for better readability and exposes a screen-reader description with the completed and total item counts.
- Validation for this accessibility increment is pending a fresh CI run. UI behavior at system-large font sizes and TalkBack still requires manual device/emulator verification; none is claimed here.


### Interactive checklist follow-up — 2026-10-12

- Completed checklist rows now use strikethrough styling as well as muted text color, making completion state easier to scan.
- CI passed for the interactive checkbox implementation before this visual follow-up; this latest styling change awaits CI.
- Large system-font, narrow-screen, and TalkBack behavior remain manual validation items and are not claimed as tested.


### Checklist accessibility and edge-case hardening — 2026-10-12

- Latest confirmed CI run [#38225533514](https://github.com/dukeoftoad/toadNOTES/actions/runs/38225533514) passed for commit `29ff4626cb8069cbe6d4b32c52e555a01accc05f` (unit tests, API 36 debug build, instrumentation-test compilation, and temporary test signing).
- Checklist checkboxes now expose the item text and its completion state through Compose accessibility semantics, instead of announcing only a bare checkbox.
- Added JVM regression coverage for idempotent checkbox state updates, trailing-newline preservation, and ignoring non-checklist lines.
- The accessibility and test additions after the confirmed run are awaiting CI. TalkBack, large-font, and narrow-device checks still require a real emulator/device run.


### Backup recovery regression pass — 2026-10-12

- CI run [#38226281995](https://github.com/dukeoftoad/toadNOTES/actions/runs/38226281995) passed on commit `4eb1babbcae60395772f0fd85144e10768dfbe44`, including unit tests, API 36 debug build, instrumentation-test compilation, and test APK signing.
- Added unit tests to verify backup imports reject records missing required identity fields, invalid IDs, and malformed versioned envelopes.
- Added a merge regression test proving newer copies update existing notes, older copies do not overwrite them, new IDs are added, and existing notes absent from the import are retained.
- Marked the interactive checklist implementation milestone complete in ROADMAP.md based on passing CI; real-device interaction, TalkBack, and font-scaling checks remain unverified.
- The new backup/merge tests and this log update await CI validation. PR #7 remains open and unmerged.



### Phase 6 backup/merge validation — 2026-10-12

- Android CI run [#38226761119](https://github.com/dukeoftoad/toadNOTES/actions/runs/38226761119) passed on commit `404cc744eccbb3d6854a93340136d2a793eb70a0`, including unit tests, API 36 debug build, instrumentation-test compilation, and test APK signing.
- Updated release-checklist and roadmap statuses for backup validation, versioned/legacy import, local corruption recovery, and interactive checklist coverage. Device-only accessibility and interaction checks remain open.
- Added a merge regression case for equal timestamps: an import with a timestamp tie must not overwrite the existing copy, and existing note ordering must remain stable while new IDs append.
- This new equal-timestamp regression test and documentation updates require a fresh CI run. PR #7 remains open and unmerged.


### Final Phase 6 CI verification — 2026-10-12

- Android CI run [#38228131828](https://github.com/dukeoftoad/toadNOTES/actions/runs/38228131828) completed successfully on commit `48fd9d36b3ca4a559615bf74d68a149eaeede91c` (unit tests, API 36 debug build, instrumentation-test compilation, temporary test signing, and artifact uploads).
- The run validates the stale-editor-version guard and current backup/merge regressions on that exact code head.
- Documentation now records this CI result. The documentation updates themselves will trigger another CI run before merge.
- Not run: emulator/physical-device smoke tests, TalkBack, larger font/display scaling, keyboard/focus/insets, production signing-certificate verification, release/minified build, and installed upgrade testing. No production-release readiness is claimed.
- PR #7 remains open until the documentation commit also passes CI; merge will be performed only after that final check.


### APK CI and release-page documentation — 2026-10-12

- Documented the GitHub Releases download page and version-tag publishing steps in README.md.
- Added explicit release-delivery gates to RELEASE_CHECKLIST.md: minified release build, tag-only publication, permanent signing secrets, fail-closed signing, artifact naming, signature/fingerprint verification, and install validation.
- The `security/privacy-hardening` branch already configures Android CI to build the minified release variant without disposable test signing, and Android Release to publish only version-tagged APKs signed with the permanent Actions secrets.
- Validation status: documentation/workflow changes require the branch CI run; no production APK was published by this documentation update. A successful build alone does not replace device and certificate-fingerprint validation.


## Documentation refresh and current repository state — 2026-10-12

This section supersedes stale status snapshots at the top of this file; older entries remain as historical records and should not be read as the current PR state.

- PR #7, Phase 6 reliability, recovery, and accessibility, was squash-merged to main at 7d49fa0621a56d9c55f37bb164ea17ed61820a88 on 2026-10-12.
- PR #6, security hardening and production release pipeline, was squash-merged to main at a954879c4c409a69db9a00be98d4e279365ec3bd on 2026-10-12.
- Current baseline includes encrypted local note storage using Android Keystore/AES-GCM, guarded legacy-data migration/recovery, validated versioned backups with legacy import support, editor stale-draft protection, checklist accessibility improvements, minified release compilation, and tag-triggered production signing/publishing.
- Documentation refreshed in this branch: README.md, DEVELOPMENT.md, DEVELOPMENT_LOG.md, RELEASE_CHECKLIST.md, plus current-state summaries in ROADMAP.md and WORKLOG.md.
- Production APK status is not confirmed as published by this documentation change. A GitHub Release or an older CI-signed artifact is not evidence that the latest APK uses the permanent production certificate.
- Not claimed complete without recorded evidence: physical-device/emulator regression checks, TalkBack and large-font checks, legacy-note migration and Keystore recovery testing, independently verified production certificate fingerprint, and installation/upgrade testing of the exact published APK.
- CI for this documentation branch must be checked before merge. This work updates docs only and does not trigger a version tag or publish an APK.

Reference links: [PR #6](https://github.com/dukeoftoad/toadNOTES/pull/6), [PR #7](https://github.com/dukeoftoad/toadNOTES/pull/7), [Android CI](https://github.com/dukeoftoad/toadNOTES/actions/workflows/android-ci.yml), [Android Release](https://github.com/dukeoftoad/toadNOTES/actions/workflows/android-release.yml), [GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases).


## 2026-10-12 — UI/UX direction approved; P0 editor redesign queued
- Product owner approved the prototype direction: a focused note editor with a right-side animated Appearance panel, wallpaper previews, colour palette, visible selection states, live draft preview and predictable Apply/Cancel behavior.
- Set the UI/UX policy in `UI_UX_RULES.md` as the required contract for all future UI work and reviews.
- Roadmap priority updated: P0 is note creation/editing and the appearance panel; P1 is a full app-wide UI/UX and accessibility audit; P2 is backend/new-feature expansion using the same reusable design system.
- Source inspection confirms the current `NoteEditorContent` opens `AppearancePickerSheet`, implemented with `ModalBottomSheet`; this is a known mismatch with the approved target design.
- The latest previously checked Android CI run passed for `9aa3e32980e101f95400d29930c777a9d29aba7f`. This is baseline evidence only, not validation of the planned redesign.
- No editor redesign is claimed complete by this documentation update. Implementation and visual/device QA remain outstanding.
- Next action: implement the shared animated right-side panel and editor layout on `feature/phase-7-dashboard-ux`, preserve existing save/discard and persistence behavior, then run CI against the exact final commit.


## 2026-10-12 — P0 Appearance panel implementation started
- Replaced the editor's `AppearancePickerSheet` / `ModalBottomSheet` call with a reusable `AppearanceSidePanel` composable in `MainActivity.kt`.
- Added a right-edge slide/fade transition, dimmed dismissible scrim, system Back dismissal, close control, live colour/background preview, selected-state borders/checkmarks, labelled swatches and wallpaper preview rows.
- Panel is constrained to a phone-friendly overlay width with a wider-screen maximum; selections still update the editor draft and persist only through the existing Save note flow.
- Added test tags for the panel, scrim, preview, close action, colour swatches and background choices to support UI verification.
- Preserved the existing editor state, save path and unsaved-change handling; this is the first implementation increment, not completion of the full editor redesign.
- Commit: `f9104271c2def154931fdc858439d04f4dbd3d11`.
- Validation pending: Android CI must pass for this exact commit. Device/visual, keyboard/insets, reduced-motion, large-font and TalkBack checks have not yet been performed.
- Next: inspect CI results, fix any compile/test failures, then continue editor hierarchy/toolbar polish and add/adjust automated coverage for panel interactions.


## 2026-10-12 — P0 editor implementation continued
- Confirmed Android CI passed for appearance-panel implementation commit `054a435aceb68f4b1c2a390b78a900db46eb9429`: unit tests, API 36 debug APK build, minified release compilation, and instrumentation-test compilation all succeeded. This CI run predates the newest toolbar polish commit.
- Added a more deliberate formatting toolbar treatment in `NoteEditorContent`: grouped checklist and bullet actions in a tonal rounded surface, clearer action labels, outline treatment, and bounded word/count metadata to reduce visual clutter.
- Latest implementation commit: `49b04452219ad1ae8fa9a0799905308b2375a14e`.
- Validation for the toolbar commit is pending. No device-level visual, keyboard, reduced-motion or TalkBack testing is claimed.
- Remaining P0 work: inspect the quick-dialog editor path for duplicated editor UI, complete the create/edit interaction audit, and validate the appearance panel on actual narrow/wide layouts before calling the editor milestone complete.


### P0 editor audit — shared Quick Dialog path (2026-10-13)

- Audited the three configured editor modes and confirmed Quick Dialog used a separate `QuickNoteFields` implementation that omitted appearance, category editing, pin/reminder actions, checklist/bullet tools, sharing/copy, and the shared stale-draft/save behavior.
- Replaced the reduced-feature Quick Dialog form with the existing `NoteEditorContent` inside a bounded, rounded dialog surface. Added a compact layout option for spacing/body-field sizing and removed the duplicate `QuickNoteFields` implementation.
- This keeps the user-selected Quick Dialog mode while routing create/edit through the same editor state and note-save boundary as sheet/full-screen modes.
- Commit: `f0a1aa2a420ebe8cefd1f482ed6cb9bfb4451fcb`.
- Validation status: waiting for Android CI on the updated branch head. Source diff was reviewed; no emulator/device visual, keyboard/insets, TalkBack, or large-font test is claimed.
- Follow-up audit: validate the compact dialog's available height with the IME visible, and implement explicit Appearance Apply/Cancel semantics so closing the panel can discard uncommitted appearance-only selections without discarding unrelated note edits.


### Appearance draft cancellation semantics — 2026-10-13

- Added explicit **Apply** and **Cancel** actions to the right-side Appearance panel.
- Colour/background selections still preview immediately in the editor draft. Opening the panel records its starting appearance; Cancel, close, outside tap, and system Back restore those appearance values, while Apply retains the draft selections for the existing Save note flow.
- This cancellation is intentionally scoped to appearance selections; it does not silently discard other editor fields.
- Commit: `4564b72b24e9a6ff0217da032542ce46519b09fc`.
- Validation pending for the final branch head. Device-level interaction, keyboard/insets, motion accessibility and TalkBack checks remain unperformed.


### Appearance UI test coverage — 2026-10-13

- Updated the instrumentation test to match the panel's actual section labels and Apply action.
- Added an end-to-end regression test proving Cancel restores the original colour/background draft before the note is saved.
- The existing appearance persistence test now exercises Apply followed by the normal Save note flow.
- Commit: `3d06781d1200e5d204306ebf6d3214ba9b47d8e2`.
- CI must compile and execute the updated test where supported; visual/device testing is still outstanding.

### Appearance cancellation test selector hardening — 2026-10-13

- Added the dedicated `appearance_cancel` test tag to the Appearance panel's Cancel button and updated the regression test to target it directly, avoiding ambiguity with the editor's separate Cancel action.
- Commits: `4bbedecdf427c074561a550dab4fe28e75ec2f4d` (panel test tag), `8e68844a0e95d88f1bbf09e8193d95660dd1decd` (test selector).
- Android CI for the latest branch head is pending; device-level visual, keyboard/insets, and accessibility checks are still not claimed.

### Instrumentation execution added to required CI — 2026-10-13

- Added a dedicated API 36 / x86_64 Pixel 2 emulator job to `.github/workflows/android-ci.yml` running `gradle --no-daemon connectedDebugAndroidTest`, rather than only compiling the instrumentation test APK.
- Updated README and DEVELOPMENT guidance so they accurately describe the new automated emulator run and keep physical-device/accessibility checks separate.
- CI result for this workflow change and the latest branch head is pending; inspect the emulator job before treating this as validated.
- Device-level TalkBack, visual contrast, large-font, keyboard/insets, and physical-device checks remain unperformed.


### CI cost adjustment — 2026-10-13

- Owner requested removal of the hosted Android emulator job because they do not have a subscription/budget for that execution path.
- Removed the `instrumentation-tests` API 36 emulator job from `.github/workflows/android-ci.yml`; required CI continues to run JVM unit tests, debug/release builds, and instrumentation-test compilation.
- Updated README and DEVELOPMENT.md to distinguish compilation from actual device execution and to document `connectedDebugAndroidTest` as an optional local command when a local emulator/device is available.
- The previous emulator-run workflow entry above is retained as history; it describes the temporary addition and is superseded by this cost adjustment. No emulator execution is claimed. PR #10 remains open and unmerged.
- Validation: the next workflow run should verify the reduced CI workflow on the updated branch head.


### Required CI after hosted-emulator removal — 2026-10-13

- **Workflow run:** https://github.com/dukeoftoad/toadNOTES/actions/runs/38315174209
- **Validated commit:** `d9d134eb72ab2bfcd5125dad69b7f3cd6c5023b2` (the workflow commit on the latest branch head when run `38315174209` started).
- **Result:** success. The `Unit tests, API 36 debug build and release compilation` job completed successfully, including unit tests, debug APK build, minified release build, and instrumentation-test compilation.
- **Cost adjustment:** hosted emulator job is removed. No emulator execution is claimed; device-level visual, keyboard/insets, TalkBack, large-font, and instrumentation execution remain unverified until run locally or on an available device.
- **Follow-up:** roadmap now records CI evidence; PR #10 remains open and unmerged. Documentation-only evidence updates are on the branch after the validated workflow head and will trigger a fresh CI run.


### Exact-head CI confirmation — 2026-10-13

- **Workflow run:** https://github.com/dukeoftoad/toadNOTES/actions/runs/38315774563
- **Validated commit:** `fb6c096e85c55d78286e02c59aeb26f49efbcd0c` (exact head at run completion).
- **Result:** success. JVM unit tests, API 36 debug APK build, minified release APK build, and Android instrumentation-test compilation all completed successfully.
- **Validation boundary:** this workflow has no hosted emulator job. Instrumentation tests were compiled but not executed; keyboard/insets, large-font, TalkBack, narrow-layout and physical-device checks remain unverified.
- PR #10 remains open and unmerged pending owner approval and outstanding P0 manual checks.


### Appearance selection semantics regression — 2026-10-13

- Added `assertIsSelected()` assertions for the chosen colour swatch and background card in `appearancePickerChangesAndPersistsColorAndWallpaper()`.
- Commit: `26608734f1bc53dc04b592c4542baf86619cbd8e`.
- CI run [#38386226195](https://github.com/dukeoftoad/toadNOTES/actions/runs/38386226195) passed on that exact commit: JVM unit tests, debug APK build, minified release build, and instrumentation-test compilation.
- Boundary: instrumentation tests were compiled, not executed on a device/emulator. The assertions still need local/device execution, as do keyboard/insets, narrow-layout, large-font, and TalkBack checks.
- Follow-up documentation commit records this evidence in ROADMAP.md. A new CI run is required for the resulting branch head. PR #10 remains open and unmerged.


### Responsive appearance panel follow-up — 2026-10-13

- Updated the editor's Appearance panel width cap to be responsive: up to 360 dp on phone-sized windows and up to 480 dp when the available editor width is at least 600 dp.
- The panel still occupies at most 88% of the available width, preserving the writing canvas as the primary surface on phones.
- Commit: `1127653ce0ecc7f139082f53b93376fae117219b`.
- Validation boundary: source change is committed; exact-head CI is pending. Wide-screen behavior has not been visually verified on a tablet/emulator, and this does not close P0 device/accessibility checks.


### Appearance swatch touch-target follow-up — 2026-10-13

- Increased each colour swatch hit target to 48 dp and enabled horizontal scrolling for the palette so the seven options do not compress below the intended touch size on narrow phones.
- Existing semantic selected state and test tags are preserved.
- Commit: `b323dbe8c37e695f014a94e3b94a7453faa89ac1`.
- Validation boundary: exact-head CI pending; swatch scrolling and touch behavior still need device/emulator verification.


### P0 appearance dismissal regression coverage — 2026-10-13

- Added instrumentation coverage for system Back and outside-scrim dismissal of the right-side Appearance panel.
- Both tests change colour/background drafts, dismiss without Apply, then save a note and assert the original appearance is preserved.
- Commit: `1f13cf49e5099fb767a358ff6a701987777c6f5b`.
- Validation: inspect the exact-head CI run below; instrumentation tests are compiled in required CI but are not executed because hosted emulator usage is intentionally excluded. These tests need execution on a local emulator/device before P0 is closed.


### P1 search regression coverage — 2026-10-13

- Added a stable `note_search_field` test tag and an instrumentation regression covering case-insensitive search against note body and category, in addition to the existing title-search coverage.
- Commits: `990ec55a88bee6dae7eb869bcc988394b4769163`, `611c7a2755aa87b2fdce7056b7f8190ba0c2a3e2`, `5072889751b544bd046c5e2b999da6a7f6fecaff`, `811f583b080f7e7f0f129a41f7f3cabb3bfbb900`.
- P0 dismissal test additions are in `1f13cf49e5099fb767a358ff6a701987777c6f5b`.
- Validation boundary: exact-head CI is running/awaiting result. Instrumentation test APK compilation does not execute these UI tests; local device/emulator execution remains required. No manual visual, keyboard/insets, or TalkBack verification is claimed.


### P0–P3 execution started — 2026-10-13

- P0: added UI regression coverage for Appearance system-Back and scrim dismissal rollback (`1f13cf49e5099fb767a358ff6a701987777c6f5b`).
- P1: added a stable dashboard search test tag and search regression coverage for case-insensitive body/category matches (`990ec55a88bee6dae7eb869bcc988394b4769163`, `611c7a2755aa87b2fdce7056b7f8190ba0c2a3e2`, `5072889751b544bd046c5e2b999da6a7f6fecaff`, `811f583b080f7e7f0f129a41f7f3cabb3bfbb900`, `e1a567cfc7aba2bf1148c14d693face03d8e1067`).
- P3: corrected `RELEASE_CHECKLIST.md` to name the workflow's actual `KEYSTORE_BASE64` secret (commit `0e15a910b23b97e61b7d77696a7b24df5b6dcb45`); no secret values were read or changed.
- Added explicit P0–P3 execution gates to `ROADMAP.md` (commit `66ffe96f5af34de2dd785714071da06c19ad10ca`).
- Validation boundary: latest-head required CI is shown below if a run is associated with the head. The workflow compiles instrumentation tests but does not execute them; no device, TalkBack, signing-certificate, or installed-APK verification is claimed.


### P3 release workflow audit follow-up — 2026-10-13

- Corrected the release checklist secret name to `KEYSTORE_BASE64`, matching `.github/workflows/android-release.yml`.
- Clarified the workflow's actual release paths: version tags publish version-named APKs; manual dispatch publishes only with a validated version tag; main pushes produce a signed workflow artifact and may repair an eligible existing release.
- Commits: `0e15a910b23b97e61b7d77696a7b24df5b6dcb45`, `60c6541a65f9fbf6e2c9471eb9bbf8a2f8f99384`.
- No keystore, alias, or password values were accessed or changed. Signature fingerprint verification and install testing remain unperformed.


### UI/UX roadmap cross-check and exact-head CI — 2026-10-13

- Cross-checked `ROADMAP.md`, `UI_UX_RULES.md`, `README.md`, and the PR #10 branch status against the active P0–P3 UI/UX sequence.
- Fixed README's stale signing-secret name from `SIGNING_KEY` to `KEYSTORE_BASE64`, matching `.github/workflows/android-release.yml` and `RELEASE_CHECKLIST.md`.
- Fixed the final instrumentation-test compilation failure by changing the single-node absence assertion to `assertDoesNotExist()` (commit `867f61f2f7f1d6baff42c8db1d3644ac126145e1`).
- Exact-head Android CI run [#38415822493](https://github.com/dukeoftoad/toadNOTES/actions/runs/38415822493) passed on that commit: unit tests, debug APK, minified release APK, and instrumentation-test compilation.
- P0 is **implementation-complete but validation-open**: instrumentation tests were compiled, not executed; keyboard/insets, compact dialog reachability, narrow/wide layouts, large fonts, reduced motion, contrast and TalkBack remain unverified. P1 app-wide audit is also still open. PR #10 remains open until those outstanding gates are addressed and owner approves merge.


### Focused CI recovery and Settings interaction audit — 2026-10-14

- Investigated Android CI run [#38520909916](https://github.com/dukeoftoad/toadNOTES/actions/runs/38520909916). Unit tests, debug APK build, and minified release APK build passed; instrumentation-test compilation failed because `assertDoesNotExist` is unavailable in this project's Compose test API.
- Replaced the unsupported single-node assertion with the already-used `onAllNodesWithText(...).assertCountEquals(0)` pattern in `CombinedDashboardFilterSortTest.kt` (commit `30b1a4104a6a43ed996f8e77cc5fca1821db3fa4`).
- During the wider Settings interaction audit, found `SwitchRow` had both a clickable parent row and an independently handled `Switch`; this could dispatch the same preference change twice when the switch itself was tapped. Changed the row to own accessible `Role.Switch` toggle semantics and made the child Switch presentation-only (`onCheckedChange = null`) (commit `240adb7479a53f127bb2dc76df7f7b9d9dd53941`).
- Validation status: a new CI run for the current head has not yet been returned by the GitHub workflow lookup. Do not treat either change as CI-verified until an exact-head run completes. No hosted emulator has been added; device execution and visual/accessibility checks remain outstanding.

### P1 Settings preference regression coverage — 2026-10-14

- Added `settingsCardMetadataTogglesUpdatePreferencesOnceAndPersist` in `NestNoteE2ETest.kt` to cover both card metadata settings: note previews and dates/reminders. The test checks the persisted values after toggling and again after Activity recreation.
- Also retained the existing search-specific empty-state assertions and the Settings row's single-owner switch semantics.
- Commit: `2f60fd1c98211f4272fe9b9074adb92ae2aa1c55`.
- Exact-head Android CI passed: [run #38526668685](https://github.com/dukeoftoad/toadNOTES/actions/runs/38526668685). The run passed unit tests, API 36 debug build, and release compilation; the workflow compiles instrumentation tests but does not execute them on a device.
- PR [#10](https://github.com/dukeoftoad/toadNOTES/pull/10) remains open and unmerged. Device validation (keyboard/insets, responsive widths, large text, contrast, reduced motion and TalkBack) remains open; no manual QA is claimed.


## Dark-theme editor contrast fix — 2026-10-15

- Replaced the editor canvas's hard-coded white fallback with the active Material theme surface, so wallpaper-backed editors no longer flash/show a white base in dark mode.
- Made editor foregrounds, toolbar icon tints, category chips, reminder surface and save-button foreground follow the active theme in dark mode while retaining the existing light-mode pastel palette.
- Change is source-reviewed only at this point. Exact-head Android CI and visual verification on a dark-theme device/emulator are still required; do not mark the whole-app P1 audit complete from this patch alone.


### UI implementation strategy — reuse-first — 2026-10-15

- User direction: prefer adapting/copying mature open-source implementations and existing Material 3 / Jetpack Compose components over creating new UI code or adding new files.
- Reuse order: (1) existing NESTNOTE components and theme tokens, (2) official AndroidX Material 3 components already in the app, (3) compatible, license-cleared open-source implementations where direct reuse offers a clear advantage.
- Tubular and Metiq were identified as examples to inspect, but their GPL licensing means their source remains reference-only until NESTNOTE's project license and compatibility are confirmed. No code from either project has been copied.
- Preserve NESTNOTE's note model, local persistence, and save/discard contracts; do not import another app's architecture wholesale.
- This is a strategy decision, not a validation claim. Continue implementation on PR #16 using existing dependencies/components; record exact-head CI and manual/device validation separately.


### Material 3 editor theme reuse — 2026-10-15

- Continued PR #16 by reusing the app's existing Material 3 color scheme for reminder/archive/more/clear icons, category-chip outlines, editor divider, disabled save-button state, and Appearance scrim instead of keeping separate hard-coded light-only values.
- Implementation commit: `17f6dac9d20332a512c8b0955568cd6fd10427b8`.
- This is source-level work only. Exact-head Android CI and device/emulator visual checks have not yet been confirmed; no accessibility/device verification is claimed.


## 2026-10-15 — Open-source Markdown preview
- Added an opt-in Markdown preview toggle to the note editor, using Markor's `GsSimpleMarkdownParser` copied from [Markor source](https://github.com/gsantner/markor/blob/master/app/src/main/java/net/gsantner/opoc/format/GsSimpleMarkdownParser.java).
- Upstream file identifies SPDX `Unlicense OR CC0-1.0` and credits Gregor Santner (2017–2025); that header is retained in `app/src/main/java/com/nestnote/app/opensource/GsSimpleMarkdownParser.java`.
- Added a small adapter that escapes raw HTML in user note text before passing it through the parser, then renders via AndroidX `HtmlCompat`. Preview is opt-in; existing Markdown/plain-text content remains unchanged in encrypted storage.
- Added parser regression tests for common formatting, lists, and links.
- Implementation commit chain on `feature/markdown-preview-markor-parser`: `b071cb8f` (vendored parser), `df7b9bed` (adapter), `d29d2627` (editor toggle), `83da445d` (typography fix), `48751ab4` (parser tests).
- Validation: Android CI has not yet been run for this branch. No emulator/device validation claimed. The upstream parser is intentionally limited to common Markdown syntax; this is not a full CommonMark implementation.


### Markdown preview CI fix — 2026-10-15

- Fixed Kotlin compilation failure in the Markdown preview's Android TextView initialization by using `setTextSize(TypedValue.COMPLEX_UNIT_SP, markdownBodySize)`, avoiding the outer editor `textSize` enum shadowing the TextView property.
- Validation before fix: Android CI run #274 failed at `MainActivity.kt:1436` with “val cannot be reassigned” and a Float/EditorTextSize type mismatch. Awaiting exact-head CI after this fix; no device validation claimed.


### Contextual Markdown heading control — 2026-10-15

- Started the editor UX milestone on a fresh branch from updated `main`, rather than trying to merge the stale audit-only PR #17 branch.
- Added a compact **H1** control beside checklist and bullet actions. It toggles a Markdown `# ` prefix on the current line, preserves indentation, and uses the existing text-selection update path; no new UI files or dependencies were added.
- Added JVM regression tests for toggling, multi-line isolation, indentation, and cursor adjustment.
- Commits: `e1c9d0a` (helper), `0ae9089` (tests), `dff5a02` (editor control).
- Validation boundary: source changes are committed; Android CI has not yet reported for this branch. No emulator/device, dark/light visual, keyboard/insets, large-text, or TalkBack validation is claimed.


### Inline formatting controls — 2026-10-15

- Extended the contextual toolbar with **Bold** and *Italic* controls, alongside H1, checklist, and bullet actions.
- Selected text is wrapped in Markdown markers and remains selected; invoking the same control on an already-wrapped selection removes the markers. With a collapsed cursor, the control inserts a marker pair and places the cursor between them.
- Added JVM tests for wrapping/unwrapping selected text and inserting a marker pair at the cursor.
- Commits: `2fa1064` (formatting helper), `178e1f2` (tests), `0691bfbb` (toolbar commit; full SHA: `0691bfbbda94c7423bfd10ab8da46ff64ae5d507`).
- Validation: exact-head Android CI remains unconfirmed; no emulator or accessibility testing claimed.


### Expanded Markdown toolbar — 2026-10-15

- Extended the existing contextual formatting row with strikethrough and inline-code actions, using the same tested selection-preserving helper as bold/italic.
- Made the toolbar horizontally scrollable so the additional controls do not force the word/character/read-time summary or formatting actions to compress into a narrow row; removed the weighted spacer that is incompatible with a horizontally scrolling row.
- Added JVM regression cases for strikethrough/code wrap-and-unwrap behavior, clamped selection bounds, and rejecting an empty marker.
- Commits: `0addc28` (toolbar actions and scroll behavior), `05a0e50` (formatting regression tests).
- Validation: Android CI was green on the previous head (`936e10e`); the new commits require a fresh exact-head CI run. No emulator/device or visual/accessibility testing claimed.


### Markdown preview safety regression tests — 2026-10-15

- Added Robolectric JVM tests for the Markdown preview boundary: raw HTML must not create image or link spans, normal Markdown links remain links, raw HTML anchors do not become links, and empty/plain-text bodies are handled.
- Test file commit: `547dbffbe2da79e7341158d411b448da6c428801`.
- Validation pending for this new commit; the earlier exact-head CI success does not validate this increment. No device or visual testing claimed.


### Markdown preview link interaction — 2026-10-16

- Source review found that Markdown links were parsed into `URLSpan`s but the preview `TextView` did not install a movement method, so the links were not reliably actionable despite being rendered as links.
- Enabled Android's `LinkMovementMethod` on the preview `TextView`; this does not change note storage, parsing, or backup formats.
- Implementation commit: `18f4d41d30c2d2bc4719ed189755ac7fce8c0fc6`.
- Exact-head CI is pending. No device/emulator or visual/accessibility validation claimed.


### Numbered Markdown lists — 2026-10-16

- Added a contextual `1.` toolbar action that toggles a numbered-list prefix on the current line, preserves indentation, and replaces an existing checklist/bullet prefix rather than stacking list markers.
- Pressing Enter on a numbered item continues the list and increments the number; Enter on an empty numbered item ends the list using the existing continuation behavior.
- Added JVM regression tests for add/remove, indentation, current-line isolation, prefix replacement, and sequential numbering including rollover from 9 to 10.
- Storage schema, encrypted persistence, backup format, and dependencies are unchanged.
- Validation: exact-head Android CI must pass after this change. No device/emulator, keyboard/insets, accessibility, or visual testing is claimed by this source update.


### Markdown link insertion — 2026-10-16

- Added a `Link` action to the existing horizontally scrollable editor toolbar. With selected text, it wraps the selection as a Markdown link label; with a collapsed cursor, it inserts an editable `[text](https://)` template.
- Places the cursor after the URL scheme so the destination can be typed immediately, while keeping the note's Markdown source editable and compatible with the existing safe preview.
- Added JVM regression coverage for selected labels and collapsed-cursor insertion. No storage schema, backup format, or dependency changes.
- Validation: exact-head Android CI is still required; no device/emulator or accessibility checks are claimed.


### Blockquote formatting — 2026-10-16

- Added a `Quote` action to toggle the Markdown `> ` prefix on the current line, preserving indentation and adjusting the cursor when adding/removing the prefix.
- Added JVM regression coverage for add/remove behavior, indentation, cursor movement, and limiting the change to the current line.
- Works with the existing Markdown source/preview pipeline; no storage or dependency changes. Exact-head CI remains pending.


### Markdown heading levels — 2026-10-16

- Generalized heading toggling to support Markdown levels 1–6, switching an existing heading level instead of stacking prefixes.
- Added H1, H2, and H3 toolbar actions with explicit accessibility descriptions; the existing horizontal scroll keeps the controls reachable on narrow screens.
- Added JVM regression coverage for switching levels, toggling a heading off, indentation, line isolation, cursor adjustment, and invalid levels.
- Existing H1 helper remains as a compatibility wrapper. Note storage, backup format, and dependencies are unchanged.
- Exact-head Android CI and device/accessibility checks remain to be verified.


## F-Droid distribution readiness — 2026-10-16

- Owner selected GPL-3.0-only. Added the complete canonical GPL v3 license text to root `LICENSE`; added a README license section.
- Added `F-DROID_METADATA_PROPOSAL.md` with package ID, candidate app summary/description, source links, and an explicitly unverified candidate build approach.
- Updated `F-DROID_DISTRIBUTION.md` with audit results, distribution gates, and evidence limits.
- Current published upstream release observed: `v1.2.4`. Do not use it as the F-Droid source revision if it predates the license file. Publish a new version tag after the licensing change is merged and validated.
- Official F-Droid metadata submission is not complete. The metadata proposal must be transferred to the official `fdroiddata` repository and reviewed; this repository PR does not create an F-Droid listing or index.
- Validation evidence: GitHub Android CI was observed in progress for the PR before the latest documentation commits. Exact-head CI must be checked again. No local F-Droid build, APK installation, reproducibility comparison, or device validation was performed in this session.
