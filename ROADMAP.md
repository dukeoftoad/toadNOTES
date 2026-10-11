# NESTNOTE Product Roadmap

Last updated: 2026-10-11

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
**Status: In progress** — [Draft PR #2](https://github.com/dukeoftoad/toadNOTES/pull/2)
1. **Audit and prepare:** inspect editor, storage, and preferences; keep one clear editor state model and preserve persistence schema.
2. **Expandable editor surface:** compact Material 3 bottom sheet that expands to a full-height writing space.
3. **Unified create/edit:** title/body editable immediately; clear Save and Cancel; existing notes open in the same editor.
4. **Draft safety:** preserve text through keyboard opening and activity recreation/rotation; confirm before discarding meaningful edits. Back and sheet dismissal must use the guard too.
5. **Validation:** focused tests, normal Android CI, fix failures, update PR. Emulator UI workflow remains manual-only and non-blocking by project decision.

Current implementation is an early increment: editor fields use saveable state and explicit close/Cancel request confirmation. Editor-surface restoration and all dismissal paths still need review. Do not mark Phase 2 complete yet.

## Phase 3 — Expand Settings
**Status: Planned; do not start until Phase 2 is complete and the user asks to proceed.**
- **Appearance:** system/light/dark theme, Material 3 accent choices, default note color.
- **Layout:** compact/comfortable density, list/grid view.
- **Note behavior:** default sorting, starting category, preview and timestamp toggles.
- **Data & privacy:** explain local storage and future export/backup controls.
- Persist preferences; audit existing preferences before considering DataStore for new ones. Avoid rewriting note storage without a migration need. Test saved choices and accessibility labels.

## Phase 4 — Improve everyday note-taking
**Status: Planned; do not start until Phase 3 is complete and the user asks to proceed.**
- Improve note colors and category handling.
- Sort by edited date, created date, and title.
- Clarify pinning and archive behavior.
- Provide clear reminder scheduling status; saved reminder metadata does not currently imply alarm/notification delivery.
- Add undo for accidental deletion.
- Test editing and persistence reliability.

## Phase 5 — Reliability and finishing touches
**Status: Planned; do not start until Phase 4 is complete and the user asks to proceed.**
- Export/import and backup/restore.
- Accessibility and larger-font support.
- Keyboard and edge-to-edge behavior.
- Tests for editing, deletion, persistence, and preferences.
- Final regression and release checks.

## Release and validation policy
- Android CI targets API 36 and is the required automated validation.
- Separate emulator UI workflow is manual-only and not a blocking check.
- Never claim physical-device testing unless it was performed.
- Record CI links and outcomes in [WORKLOG.md](WORKLOG.md).
- No automatic merge to `main`; ask the repository owner before merging a PR.
