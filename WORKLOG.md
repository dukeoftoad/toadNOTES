# NESTNOTE Work Log

Last updated: 2026-10-11

> Maintenance rule: update this log in the same commit as every implementation commit, including the commit SHA, summary, and validation status. If a separate commit is unavoidable, update the log immediately afterward.  
Repository: [dukeoftoad/toadNOTES](https://github.com/dukeoftoad/toadNOTES)

This activity record is assembled from visible repository commits, pull requests, and GitHub Actions runs. Statuses reflect the latest checks observed when written; use the linked run for live status.

## Current snapshot
- Native Android app using Kotlin, Jetpack Compose, and Material 3.
- Target SDK Android 16 / API 36; minimum SDK 26.
- main includes merged PR #1.
- Phase 2 is in progress on `feature/phase-2-note-editor`, tracked by draft PR #2. **PR #2 is not merged.**
- Existing note persistence uses SharedPreferences / NoteStore; Phase 2 preserves its format.
- Emulator UI workflow is manual-only by project decision and is not a required CI gate.
- No physical-device testing is claimed.

## Pull requests
| PR | Title | Status | Scope |
|---|---|---|---|
| [#1](https://github.com/dukeoftoad/toadNOTES/pull/1) | feat: Material 3 personalization and flexible note creation | **Merged** 2026-10-11 | Phase 1 dashboard modernization and personalization. |
| [#2](https://github.com/dukeoftoad/toadNOTES/pull/2) | feat: resilient inline note editor | **Open draft** | Early Phase 2 increment; further work and validation remain. |

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
