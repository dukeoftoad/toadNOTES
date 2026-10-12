# NESTNOTE Product Roadmap

Last updated: 2026-10-12

## Product direction
Build a calm, compact, personal note-taking experience using native Android, Jetpack Compose, and Material 3. Keep common actions fast, preserve local user data, and deliver in small, testable increments.

### Design principles
- Compact, low-friction dashboard with clear hierarchy and spacing.
- Material 3 components, adaptive color, and accessible contrast.
- Wallpaper-adaptive Material You colors on Android 12+ where available, with explicit user controls.
- Soft pastel note cards by default; optional wallpaper-adaptive card colors.
- Compact grid by default, with a list/grid preference.
- Preserve existing local note storage; avoid unnecessary schema migrations.
- Do not mark a phase complete until its relevant checks pass.

## Phase 1 — Modernize the home screen
**Status: Merged** — [PR #1](https://github.com/dukeoftoad/toadNOTES/pull/1)
- Material 3 color and typography direction.
- Compact note cards and consistent spacing; pinned notes prioritized.
- Search and category filtering; first-use empty state.
- Light/dark theme support and personalization.
- Compact/comfortable layout, sorting, pastel and adaptive note-color choices.

## Phase 2 — Create and edit notes without unnecessary navigation
**Status: Merged** — [PR #2](https://github.com/dukeoftoad/toadNOTES/pull/2)
1. **Audit and prepare:** inspect editor, storage, and preferences; keep one clear editor state model and preserve persistence schema.
2. **Expandable editor surface:** compact Material 3 bottom sheet that expands to a full-height writing space.
3. **Unified create/edit:** title/body editable immediately; clear Save and Cancel; existing notes open in the same editor.
4. **Draft safety:** preserve text through keyboard opening and activity recreation/rotation; confirm before discarding meaningful edits. Back and sheet dismissal must use the guard too.
5. **Validation:** focused tests, normal Android CI, fix failures, update PR. Emulator UI workflow remains manual-only and non-blocking by project decision.

Implemented a unified Material 3 editor with saveable title/body/category/color/reminder drafts, explicit save/cancel, discard confirmation, and configurable sheet/full-screen/quick-dialog surfaces. PR #2 is merged.

## Phase 3 — Expand Settings
**Status: Merged** — [PR #3](https://github.com/dukeoftoad/toadNOTES/pull/3)
- **Appearance:** system/light/dark theme, Material 3 accent choices, default note color.
- **Layout:** compact/comfortable density, list/grid view.
- **Note behavior:** default sorting, starting category, preview and timestamp toggles.
- **Data & privacy:** explain local storage and future export/backup controls.
- Persist preferences; audit existing preferences before considering DataStore for new ones. Avoid rewriting note storage without a migration need. Test saved choices and accessibility labels.

## Phase 4 — Improve everyday note-taking
**Status: Complete** — [PR #4 merged](https://github.com/dukeoftoad/toadNOTES/pull/4)
- [x] Improve note colors and category handling with pastel/adaptive color choices.
- [x] Sort by edited date, created date, and title; pinned notes stay first.
- [x] Clarify pinning and archive behavior: archived notes are removed from active views, remain accessible in Archived, and can be restored without losing their content.
- [x] Show clear reminder status in the editor and card metadata; explicitly disclose that reminder times are saved as metadata only and do not schedule notifications/alarms.
- [x] Add Undo for accidental deletion using the shared snackbar action; restore the deleted note's saved metadata.
- [x] Add creation timestamp persistence and regression coverage for legacy records and edits.
- [x] Align archive/restore behavior in grid and list layouts; add persistence and archive-screen restore regression coverage.
- [x] Refine editor hierarchy, tonal surfaces, writing space, close/color controls, and save feedback using the supplied references as inspiration (not pixel-perfect targets).
- [x] Redesign the full-screen editor and add the appearance picker with persisted pastel colors and wallpaper choices.
- [x] Fix screenshot-reported foreground, placeholder, cursor, toolbar, and Save/Cancel contrast; strengthen palette distinction.

**Review outcome:** owner approved Phase 4 after testing; PR #4 was squash-merged on 2026-10-12. Visual references are inspiration, not proof of pixel-perfect parity. Reminder delivery and rich-text formatting remain unimplemented. Device testing was performed by the owner; no additional post-fix device verification is claimed by the implementation agent.

## Phase 5 — Reliability and finishing touches
**Status: In progress** — PR [#5](https://github.com/dukeoftoad/toadNOTES/pull/5) was squash-merged into `main` on 2026-10-12 (merge commit `ee046eaa67fe2b2be713ef96becabf0892f034fe`). Android CI passed on the previously recorded implementation head; manual UI, accessibility/larger-font, keyboard/edge-to-edge, and final regression checks remain outstanding, so Phase 5 is not marked complete.
- [x] Backup and restore: JSON backup to a user-chosen file and merge-restore (new ids added, newest copy wins, nothing deleted). Same JSON format as local storage.
- [x] Settings redesign: segmented controls (theme, view, density, sort, text size), selectable cards (note card colors, editor mode), default-category chips, per-setting summaries, reset-to-defaults.
- [x] Editor UX: checklist and bullet tools with Enter-to-continue lists, live word count and reading time (optional), editor text size setting, category chips, reminder presets (later today, tomorrow morning, next Monday) and clear-reminder, share/copy note, new notes open focused, edited/created info.
- [x] Verify automated checks with Android CI: [run #38196257185 — success](https://github.com/dukeoftoad/toadNOTES/actions/runs/38196257185). Unit tests, API 36 debug APK build, instrumentation-test compilation, and test-APK signing passed.
- [ ] Manual emulator/device UI validation (not yet run; the emulator UI workflow is manual-only).
- [ ] Accessibility and larger-font audit.
- [ ] Keyboard and edge-to-edge behavior review.
- [ ] Tests for editing, deletion, persistence, and preferences (unit tests added for list/reminder helpers and backup merge).
- [ ] Final regression and release checks.

Still not implemented: reminder delivery (alarms/notifications), rich-text formatting beyond checklist/bullet prefixes, tap-to-toggle checkboxes inside the text.

## Release and validation policy
- Android CI targets API 36 and is the required automated validation.
- Separate emulator UI workflow is manual-only and not a blocking check.
- Never claim physical-device testing unless it was performed.
- Record CI links and outcomes in [WORKLOG.md](WORKLOG.md).
- No automatic merge to `main`; ask the repository owner before merging a PR.

---

## NESTNOTE — Project discovery and improvement plan

I reviewed the current `main` branch of [dukeoftoad/toadNOTES](https://github.com/dukeoftoad/toadNOTES), including the app architecture, note storage and backup logic, editor helpers, automated tests, Android configuration, roadmap, and work log.

My recommendation is to make the next phase about data safety, editor reliability, and a polished real-device experience before adding major new features.

NESTNOTE already has a useful foundation: native Android with Kotlin, Jetpack Compose and Material 3, local note storage, a customizable editor, categories, archive/restore, and JSON backup. The most valuable next step is to make those features more dependable as the app grows.

### 1. Current project assessment

**Product foundation — Good foundation**

The app has a coherent Material 3 design direction, local-first notes, customization, and the main create/edit/archive flows.

**Data reliability — Priority improvement**

Notes are stored as serialized JSON in SharedPreferences. Backup exists, but storage corruption, large note collections, and recovery deserve stronger protection.

**UX and accessibility — Needs validation**

The editor, appearance picker, and theme options are implemented, but larger text, keyboard behavior, contrast, and real-device regression checks remain important.

**Project maintenance — Needs cleanup**

The roadmap and work log are not fully synchronized: the work log still describes earlier PR and phase states despite subsequent merges.

### 2. Recommended improvements, in priority order

![The 8 best Android note-taking apps](https://images.openai.com/static-rsc-4/OdEal2Jn87BmAUiggcDrew2xwB9YojWHGLwkY-vS1v1iU4yYeAC6GoVYPsDQdZ2hO176TpdDnurl3LrcXO1PucAbODkZFuEns8u4OMG9466-9gIRvhgTS9EWH8Xm3hgv9xyOzMvC7NUxaehbp94XCYX69YyEQO58yRauDl7rqTUmp54hPPrCW31d4N5uL3AB?purpose=fullsize)

#### P0 — Make editing and saving dependable

**Highest priority**

The current UI logic is concentrated in `MainActivity.kt`, and note data is persisted through `NoteStore.kt`. Protect the writing experience before expanding functionality.

- Add explicit save status: Saving, Saved, and Save failed.
- Protect unsaved edits when navigating back, rotating the device, or recreating the activity.
- Prevent stale editor drafts from overwriting a newer version of the same note.
- Add tests for rapid edits, repeated saves, and interrupted editing.
- Keep recovery behavior clear when saving fails.

Files: [MainActivity.kt](https://github.com/dukeoftoad/toadNOTES/blob/main/app/src/main/java/com/nestnote/app/MainActivity.kt) · [NoteStore.kt](https://github.com/dukeoftoad/toadNOTES/blob/main/app/src/main/java/com/nestnote/app/NoteStore.kt)

![Fossify Notes Beta | F-Droid - Free and Open Source Android App Repository](https://images.openai.com/static-rsc-4/HU5BkiISd91RcEVbsaGQ1KYXwFUtMrhWxMW9SbdzvF4_Gft7ZZvB2kv4KiogEpZHaTXR5shFLpc7YcgsxvJazde85qQyfK1dDvx9jBMzdK9PpS7XZ_AoGCMOx76vaoY-Bhl51T0TyvnIGD1m2N3np6Cc2G1V6Wo7HO8oBfMe_By0hyqOo6IGtIDDg66Yyl3y?purpose=fullsize)

#### P0 — Strengthen backup and recovery

Protect user notes against accidental loss and malformed backup files.

- Validate imported note IDs, required fields, field sizes, and duplicate records.
- Add backup schema versions and a documented migration strategy.
- Make restore atomic: validate the entire import before changing local notes.
- Offer a preview of notes to add or update before applying a restore.
- Add a clear recovery path for malformed or unreadable local storage instead of silently treating every decoding failure as an empty/default dataset.

Why this matters: `NoteStore.load()` currently falls back to starter notes when stored data is missing or cannot be decoded. That behavior deserves particular care so corrupted user data is never silently mistaken for a fresh installation.

![Nextcloud Notes brought to you for your Nextcloud server, Android and iOS devices by the heroes of our community! - Nextcloud](https://images.openai.com/static-rsc-4/j6Q0TrhVYJ05v4yoVimMl5ceiPNQUq0nfA_LOyZ8aSeG8zJwZgTlrg4Ztlzwv06kd8YK3vzrwZnjgHrBAb49CCiXqtxFsoxOzldqmHytzMZWgpqVXlLoS2o3_Cy85N278gqXoHQlS6exVZ8ahCuYw4jR_RExd61JiNNUrD3f-g242s8ES-J1HqGpxCZhpjTx?purpose=fullsize)

#### P1 — Improve the note editor

Move from text-prefix formatting toward a more natural writing experience.

- Make checklist items interactive directly in the note body.
- Preserve cursor position and selection during list formatting.
- Improve keyboard insets, scrolling, and focus handling.
- Add a reliable autosave or draft-recovery mechanism.
- Add accessibility descriptions for icon-only actions and visible keyboard focus where appropriate.

Rich-text formatting can come later; reliable plain-text editing and checklists are the better first milestone.

![Simple Notes APK Download for Android - Latest Version](https://images.openai.com/static-rsc-4/T7JEhhwesCc6Rhqa-G523dZksYMNe4PR4k-1VzvX99xSRzjFp304OMjwHxqp8HwdMDktMuNps9GGFf8W6ZdjyYsZaubEtxGn24DU5tlItZ2E6dRjh4cq-2JjSsEWD86UXHLLVZ8psEpn6AtZg1_8DGLVDQgsr-_88ke05QV9XMwWVqSA15M_4qO-fv4BTD40?purpose=fullsize)

#### P1 — Finish the visual system

Preserve the existing design direction while making it consistent across screens.

- Audit foreground/background contrast for every note color in light and dark themes.
- Test all appearance-picker choices and selected states.
- Verify larger system fonts, display scaling, and narrow screens.
- Standardize button heights, icon sizes, spacing, corner radii, and empty states.
- Ensure archived notes, dialogs, menus, and settings use the same visual language as the editor.

**Acceptance criterion:** every supported color/theme combination remains readable, and important actions remain accessible without relying on color alone.

![easynotes下载-easynotes无广告版下载 v1.3.14.06102025新版-生活服务 - 热音](https://images.openai.com/static-rsc-4/dBIc8EehqkKbQDp0begGXVbWW7m74PyyKjqeVnyNTczq0YIdlyQaZJirLKT8T7UZA8C9RsoEz6PJzTGKD_hwzeAkb91JYpCU4dwX9vJYin6jLm3FoY5w2MLXZf7rkM7TJmCf4jnWKoXklb97KgiQCyjYQeV434wb7_AKkndVe8ur4DccUD7k1q1XBdp444ov?purpose=fullsize)

#### P1 — Improve finding and organizing notes

- Add search across titles and bodies with predictable case-insensitive matching.
- Allow category and archive filters to combine with search and sorting.
- Add multi-select actions for archive, delete, and category changes.
- Make pinning and archive status visually distinct.
- Add a recently deleted area or another deliberate recovery mechanism if users need protection beyond immediate Undo.

These improvements will make the existing feature set more useful before introducing complex new capabilities.

![VerveDo | F-Droid - Free and Open Source Android App Repository](https://images.openai.com/static-rsc-4/ed-SBHVZJnc8yhCOg9-xmP2ONYMrzcDRQjbHwmS6uxYiJ0JW2zKXoi4r5NDCJrMh88B_MzeHFk-x4HIO01gOqyZxBrYRrJSJDV2D3fQG5pJd9o1bjXbTYNv_CE0uVxe1THADgQgltYckoZXv8_cAAiIIQcPNtE9Z3sXhUnZzZW2kT-5h35Ea5P2bHxBf2Nru?purpose=fullsize)

#### P2 — Improve the architecture and scalability

- Extract note persistence behind a repository interface.
- Move editor and dashboard state into lifecycle-aware ViewModels.
- Introduce Room when the data/query requirements justify a database migration.
- Separate design tokens, reusable components, settings, and editor screens.
- Keep the current storage format readable during migration, with explicit rollback and backup tests.

I would not rewrite the whole application now. Introduce these boundaries incrementally, starting with storage and editor state.

### 3. Quality and release engineering

The project has unit tests and Compose instrumentation tests, and its documented CI checks cover builds and test compilation. The next step is to test behavior more deeply, not just ensure the code compiles.

| Area | Suggested improvement |
| --- | --- |
| Unit tests | Corrupted backups, duplicate IDs, empty titles, timestamp conflicts, malformed records |
| UI tests | Create, edit, save, discard, archive, restore, and settings persistence |
| Accessibility | TalkBack labels, font scaling, contrast, touch target sizes |
| Device testing | Keyboard, rotation, activity recreation, system bars, Android versions |
| CI | Run tests on pull requests and report failures clearly |
| Release readiness | Versioned backups, release notes, privacy disclosures, install/upgrade testing |

One particularly useful improvement is to introduce a release checklist. A successful build should not automatically mean the product is ready to release.

### 4. Fix the project documentation drift

This is a small but important maintenance issue found in the current files.

- The [ROADMAP.md](https://github.com/dukeoftoad/toadNOTES/blob/main/ROADMAP.md) reflects later project progress.
- The [WORKLOG.md](https://github.com/dukeoftoad/toadNOTES/blob/main/WORKLOG.md) still contains older snapshot information, including references to Phase 4 being open and earlier PR merge states.

Reconcile the work log with the current `main` branch, including PR #4 and PR #5, and use a single current-state summary in both documents. Keep historical entries, but label them clearly as historical.

### 5. Proposed next phase

I recommend a focused **Phase 6 — Reliability, Recovery & Accessibility**, rather than adding cloud sync or a large collection of new features immediately.

#### Phase 6 candidate checklist

- [ ] Audit local storage and save failure handling — **P0**
- [ ] Harden backup validation and atomic restore — **P0**
- [ ] Add editor lifecycle and draft-recovery tests — **P0**
- [ ] Complete contrast, font-scaling, and keyboard checks — **P1**
- [ ] Make checklist interactions more natural — **P1**
- [ ] Synchronize roadmap, work log, and release checklist — **P1**
- [ ] Extract repository and ViewModel boundaries incrementally — **P2**

### What I would do first

- Audit `NoteStore` and all save/restore paths for data-loss risks.
- Add focused regression tests before modifying the storage implementation.
- Improve editor state handling and backup recovery.
- Run the automated suite and validate the interface on an emulator or physical device.
- Update the documentation and open a small, reviewable PR.

I would defer cloud sync, accounts, collaboration, and rich-text editing until local data safety and the everyday writing experience are dependable. Those are worthwhile future directions, but they introduce substantially more complexity.

**Recommendation:** start Phase 6 with a storage-and-editor reliability audit, then implement the fixes in small, testable PRs. That offers the greatest improvement to NESTNOTE without unnecessarily destabilizing the existing app.

