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
