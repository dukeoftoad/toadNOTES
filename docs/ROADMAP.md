# NESTNOTE Product Roadmap

Last updated: 2026-10-19

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

### Current redesign follow-up — 2026-10-19

The supplied HTML redesign is implemented as a native Compose adaptation. The remaining work is tracked in [docs/PROTOTYPE_PARITY_AUDIT.md](docs/PROTOTYPE_PARITY_AUDIT.md): device validation of the editor and rail, accessibility and keyboard/inset checks, cold-start and editor performance profiling, focused interaction coverage, and resolution of the small dashboard/More parity decisions. The prototype is a reference artifact, not production code.

## Private Vault — PR #88 (in progress)

- Add swipe-down entry from the Home title area and a password setup/unlock dialog.
- Persist a private-note flag in encrypted local storage and versioned/plain-array JSON backup formats; older records default to visible notes.
- Exclude private notes from normal views and provide an editor More-menu action to hide/reveal them while the vault is unlocked.
- Password gate uses a random salt and PBKDF2-HMAC-SHA256; the vault password is not an encryption key. Exported JSON backups remain plaintext.
- Required validation before merge: Android CI, password-hash tests, note persistence/backup tests, view-filter tests, and manual gesture/lifecycle checks.

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


## Sharing follow-up — local QR and encrypted links

- [ ] **Browser link sharing** — tracked in [issue #57](https://github.com/dukeoftoad/toadNOTES/issues/57). Generate a unique URL that opens in Chrome/Brave and decrypts note content on the recipient device. Never upload plaintext or the decryption key. Prefer free, low-maintenance hosting; document link revocation/expiry and URL-size limitations honestly. Do not present the feature as available until the recipient page is deployed and the end-to-end flow is tested.
- [x] **Local QR sharing** — an offline alternative only; it contains readable note text, not an encrypted link. The dialog warns recipients that the payload is plaintext.
- [x] Add a local QR-code option to the editor's sharing menu. It encodes the same share-safe note text used by Android sharing and displays a clear warning that scanned content is plaintext. QR generation does not require network access or a hosting account.

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

#### Phase 6 — Reliability, Recovery & Accessibility

**Status: In progress on [PR #7](https://github.com/dukeoftoad/toadNOTES/pull/7), branch `feature/phase-6-release-checklist`.** PR #6 security hardening remains separate and on hold. Do not merge either PR without explicit owner approval.

Phase 6 follows **P0 → P1 → P2**. Check items only after the latest relevant code and tests pass CI; device-only acceptance criteria remain unchecked until manually verified.

### P0 — Protect writing, saving, and recovery
- [ ] Validate local save failure handling and ensure failed saves never close the editor or falsely report success (implementation started; CI passed on an earlier storage/backup commit, latest editor safeguards still need CI).
- [x] Reject malformed, oversized, duplicate-ID, invalid-ID, and invalid-timestamp backup records before modifying local notes (backup validation regression tests pass CI run #38226761119).
- [x] Distinguish a fresh install from unreadable local storage; preserve corrupt bytes and offer a deliberate recovery route rather than silently loading starter notes (regression test passes CI run #38226761119).
- [x] Export versioned backups and retain legacy array import compatibility; preview additions/updates and validate the complete restore before applying it (implementation and regression tests pass CI run #38226761119).
- [x] Preserve editor drafts and discard-warning state across activity recreation; reject stale drafts that would overwrite a newer note version (version-guard unit test and CI run #38228131828 passed).
- [ ] Add/verify rapid-edit, repeated-save, interrupted-edit, malformed restore, and save-failure regression tests.
- [x] Run Android CI successfully on current PR head `48fd9d36b3ca4a559615bf74d68a149eaeede91c` (run #38228131828 passed). Focused emulator/device smoke test remains outstanding.

### P1 — Improve the everyday writing and organizing experience
- [x] Make checklist items interactive in the note body and preserve cursor/selection during list formatting (implementation and JVM tests pass CI; device interaction still needs manual validation).
- [ ] Verify keyboard insets, scrolling, focus, and narrow-screen behavior.
- [ ] Audit every note palette in light/dark themes for contrast; test appearance-picker selection states, font/display scaling, TalkBack labels, and touch targets.
- [ ] Verify case-insensitive title/body search composes predictably with category/archive filters and sorting; add multi-select actions only after core flows remain stable.
- [ ] Reconcile current-state summaries in ROADMAP.md, WORKLOG.md, and RELEASE_CHECKLIST.md while preserving older work-log entries as historical records.

### P2 — Incrementally improve architecture
- [ ] Extract note persistence behind a repository interface and cover it with focused tests.
- [ ] Move editor/dashboard state into lifecycle-aware ViewModels without a full application rewrite.
- [ ] Evaluate Room only if data/query needs justify it; design and test migration, backup, upgrade, and rollback before changing storage.

Defer cloud sync, accounts, collaboration, and rich-text editing until local data safety and the everyday writing experience are dependable.

---

## Security & Privacy Hardening

**Status: Planned — verify each finding against the current implementation before considering it confirmed or resolved.**

### Security assessment summary

Based on the repository audit of the main branch and changes proposed in PR #4, NESTNOTE is a native Android notes app built with Kotlin and Jetpack Compose. The assessed implementation stores note data locally using SharedPreferences. The reviewed codebase appeared relatively small, with no obvious critical issues such as hardcoded secrets, network calls, unsafe reflection, or dynamic code loading in the reviewed areas. Verify each observation against current source and release workflows before implementation.

The recommendation is to strengthen data-at-rest protection and release safeguards before encouraging users to store sensitive personal notes.

### Findings by severity

#### 🔴 High — Insecure local storage of notes

**Location:** app/src/main/java/com/nestnote/app/NoteStore.kt (assessment cited lines 8–20; verify line numbers against current main).

**Issue:** Notes are serialized as JSON and stored in a plain SharedPreferences file (nestnote_local) without application-level encryption.

**Potential impact:** A person with root access, a compromised device, or access to an exposed device backup/ADB workflow may be able to extract note contents, including titles, bodies, reminders, and categories. This is a local-device threat; it does not by itself imply remote access.

**Recommendation:**
- Evaluate AndroidX security-crypto support and maintenance status before choosing an encryption approach. Compare an encrypted preferences migration with a dedicated encrypted database such as SQLCipher.
- Design a backward-compatible migration from existing plaintext records; do not overwrite or delete old data until encrypted data has been written, verified, and a recovery path is available.
- Keep user-controlled JSON export/restore behavior explicit and explain that exported files are readable unless the export format is separately encrypted.
- If encryption is deferred, clearly disclose the local-storage threat model and avoid claiming that notes are encrypted at rest.

**Acceptance criteria:**
- The selected storage protection and threat model are documented.
- Existing notes migrate without loss, with tests for first launch, interrupted migration, malformed legacy data, and repeated launches.
- No plaintext copy is silently left behind after successful migration; cleanup occurs only after verified migration and a documented recovery strategy.
- Backup/export compatibility and recovery are tested, and the privacy disclosure accurately describes what is and is not encrypted.

#### 🟠 Medium — Android automatic backup may include note data

**Location:** app/src/main/AndroidManifest.xml (android:allowBackup="true" was reported by the assessment; verify the current manifest and backup rules).

**Issue:** Android automatic backup behavior may include app SharedPreferences, depending on platform version and configured backup/data-extraction rules.

**Potential impact:** Note data could be copied into a cloud or device backup outside the app's explicit export flow.

**Recommendation:**
- Decide whether automatic system backup is an intentional product feature.
- If it is not intended, disable automatic backup and configure applicable Android backup/data-extraction rules to exclude sensitive note storage.
- If backup is intended, document the behavior and assess the security of the destination and restore path. Keep user-controlled export separate from automatic backup policy.

**Acceptance criteria:**
- The chosen backup policy is documented and matches the manifest and applicable backup-rule files.
- Tests or repeatable device validation confirm whether note storage is included or excluded as intended across supported Android versions.
- Disabling backup does not remove or break the in-app user-controlled JSON export/restore feature.
- Privacy documentation explains actual backup behavior without overstating protection.

#### 🟠 Medium — Note input lacks explicit validation limits

**Location:** app/src/main/java/com/nestnote/app/NoteStore.kt (load/save paths; verify current implementation).

**Issue:** The assessment reports that titles, bodies, and categories are persisted and loaded without sufficient explicit validation or size limits.

**Potential impact:** In the current local-only app, direct injection risk is limited because no WebView or network processing was found in the reviewed areas. Oversized or malformed data can still cause reliability problems. If notes are later rendered in web content, exported to other formats, or synced, context-specific injection risks may arise.

**Recommendation:**
- Define reasonable, documented field-size limits and validate required fields, IDs, categories, timestamps, and duplicate records at the storage/backup boundary.
- Reject malformed imports safely and validate the entire import before modifying stored notes.
- Do not strip or rewrite ordinary user text unnecessarily. Apply context-appropriate escaping only when rendering or exporting into a format that requires it; input validation is not a substitute for output encoding.

**Acceptance criteria:**
- Validation rules are centralized and documented, including legacy-record handling.
- Tests cover empty/oversized fields, malformed JSON, missing required fields, duplicate IDs, invalid timestamps, and large imports.
- A rejected import leaves existing local notes unchanged and provides a clear error to the user.
- Normal Unicode and multiline note content continue to round-trip without unwanted modification.

#### 🟡 Low — Release minification and obfuscation are not enabled

**Location:** app/build.gradle.kts (release build configuration; verify current settings).

**Issue:** The assessment reports that release builds do not enable R8/ProGuard minification or resource shrinking.

**Potential impact:** The APK may be easier to inspect or reverse-engineer. Obfuscation raises the effort required for analysis but is not a replacement for secure storage, sound authorization, or secret management.

**Recommendation:**
- Evaluate enabling isMinifyEnabled = true and isShrinkResources = true for release builds, with an appropriate ProGuard/R8 rules file where required.
- Keep debug builds easy to diagnose and ensure release builds retain needed Android/Compose functionality.
- Preserve mapping files securely for crash diagnosis and do not publish them unintentionally.

**Acceptance criteria:**
- A release variant builds successfully with the intended R8/resource-shrinking configuration.
- Unit/instrumentation coverage and a manual smoke test cover note creation/editing, backup/restore, settings, and app launch in the minified build.
- Required keep rules are minimal and justified; mapping-file handling is documented.

#### 🟡 Low — Release workflow uses a temporary signing key

**Location:** .github/workflows/android-release.yml (assessment cited lines 11–23; verify current workflow).

**Issue:** The assessment reports that the release workflow generates a temporary keystore. This can be acceptable for CI validation, but not for publishing production updates that need a stable signing identity.

**Potential impact:** Builds signed with a newly generated key cannot serve as seamless updates to an app installed with a different signing key. Users may need to uninstall and reinstall, risking local data loss.

**Recommendation:**
- Keep ephemeral signing for non-distribution CI artifacts only, if needed.
- Before public distribution, configure a permanent app-signing key through GitHub Secrets or a managed signing service. The assessment reports secret names SIGNING_KEY, KEY_ALIAS, KEY_STORE_PASSWORD, and KEY_PASSWORD; verify the workflow's expected formats and variable names before configuring them.
- Restrict access to signing secrets, avoid printing them in logs, and document key backup and rotation/recovery procedures.
- Make tagged production releases fail closed if production signing configuration is missing; never silently fall back to a temporary key for a public release.

**Acceptance criteria:**
- CI validation builds and production release signing are distinct, explicit paths.
- A tagged production release cannot be published unless configured permanent signing credentials are present and valid.
- The resulting artifact's signer is verified against the expected certificate.
- Signing secrets and keystore material never appear in repository files, build logs, or generated artifacts.
- The signing-key backup, access, and recovery procedure is documented before the first public release.

### Positive observations from the assessment

These are observations from the reviewed scope, not a guarantee that every file and dependency has been exhaustively audited. Recheck them when the implementation or dependency set changes.

- **No hardcoded secrets found:** Searches for password, secret, api_key, and token in MainActivity.kt and NoteStore.kt reportedly returned no matches.
- **No network permissions or network calls reported:** The manifest was reported to request no permissions, and the reviewed code reportedly contains no network calls, WebViews, or dynamic code loading.
- **Small exposed-component surface reported:** MainActivity is the only exported component noted in the assessment and serves as the launcher. Verify the merged manifest and component declarations when adding features.
- **PR #4 migration is backward-compatible:** The creation timestamp decoding expression was reported as o.optLong("created", o.optLong("id", …)), allowing older records without created to fall back to an identifier-derived value. Preserve and test this compatibility when changing storage or backup formats.

### PR #4-specific security notes

PR #4 introduced a created field to the note model and updated JSON serialization.

- **Security regression:** The assessment found no new storage location or network activity attributable to this change.
- **Backward compatibility:** The reported fallback for older records without created supports legacy data; retain regression tests for these records.
- **Test coverage:** The assessment reports that NoteStoreTest.kt and NestNoteE2ETest.kt cover persistence of the new field. Verify the exact tests on current main before relying on this statement.
- **Backup/export:** Ensure created is preserved by every current and future export, import, merge, migration, and restore path.

### Prioritized implementation plan

- [ ] **P0 — Verify findings against current main.** Inspect NoteStore.kt, the merged manifest and backup rules, release build configuration, and .github/workflows/android-release.yml. Record exact paths and line references; update any finding that no longer applies.
- [ ] **P0 — Define the local-data threat model and storage-protection decision.** Document encryption options and migration/recovery risks before selecting an implementation.
- [ ] **P0 — Prevent silent data loss during storage changes.** Add regression tests for malformed local data, interrupted migration, backup/restore, and recovery before changing persistence.
- [ ] **P0 — Set an explicit Android automatic-backup policy.** Decide whether to exclude note data or support system backup intentionally, and validate manifest/rules on supported API levels.
- [ ] **P1 — Add storage and import validation.** Centralize field limits and schema validation; reject invalid imports atomically and preserve valid Unicode/multiline content.
- [ ] **P1 — Harden production release signing.** Separate CI signing from production signing and require stable signing credentials for tagged releases before public distribution.
- [ ] **P1 — Evaluate R8 and resource shrinking.** Enable them for release only after a minified-release smoke test and regression suite pass.
- [ ] **P1 — Update privacy and release documentation.** Explain local storage, encryption status, automatic backup behavior, user-controlled exports, and signing/release safeguards accurately.
- [ ] **P1 — Add a security release checklist.** Include dependency/configuration review, manifest inspection, backup/restore tests, release signer verification, and installation/upgrade validation.

### Security release gate

Before a public release intended for sensitive personal notes:

1. All P0 findings are remediated or have a documented, owner-approved risk decision.
2. Existing-note migration and backup/restore have passed regression tests with no silent data loss.
3. Automatic backup behavior is verified and disclosed.
4. A production artifact is signed with the expected stable certificate; temporary CI keys are not used for distribution.
5. The release variant builds and passes the agreed regression/smoke tests, including if R8 is enabled.
6. User-facing privacy documentation matches the shipped implementation.

**Overall assessment:** NESTNOTE has a promising local-first MVP foundation, but the reviewed data-at-rest and release-process controls should be strengthened before recommending it for sensitive personal information. Treat severity as a prioritization aid, verify each observation against current source, and do not mark a finding resolved until its acceptance criteria have been tested.



### Phase 6 validation update — 2026-10-12

- Android CI run [#38228131828](https://github.com/dukeoftoad/toadNOTES/actions/runs/38228131828) passed on commit `48fd9d36b3ca4a559615bf74d68a149eaeede91c`: unit tests, API 36 debug APK build, instrumentation-test compilation, temporary test signing, and artifact uploads.
- The latest editor stale-version guard is CI-validated. Rapid/repeated-save scenarios and full device/emulator lifecycle/accessibility checks remain outstanding.
- PR #7 is still open pending final documentation validation and owner-requested merge workflow. This CI signing is for test artifacts only, not production release signing.


## Documentation reconciliation — 2026-10-12

**Current merged baseline:** PRs #1–#7 are merged into main, including Phase 6 reliability/recovery/accessibility work and PR #6 security/privacy hardening. Older dated notes and statuses above are retained as historical records; when they conflict with this current snapshot, this section is authoritative.

### Current implementation baseline
- Local note payload encryption uses AES-GCM and an Android Keystore key; legacy plaintext migration is designed to commit encrypted content before deleting the old value.
- Backup/restore validates a versioned schema, accepts legacy JSON arrays, previews changes, merges by ID, and preserves notes absent from the backup.
- Editor stale-draft protection and interactive checklist behavior have automated regression coverage.
- Release build uses R8/resource shrinking. Production APK publishing is intended for version tags and permanent signing secrets only; historical CI-signed artifacts are not production proof.

### Next priorities
1. Validate legacy-data migration, Keystore failure/recovery, and backup policy on supported Android versions and a real device.
2. Run editor/dashboard regression checks for keyboard/insets, focus, larger fonts, narrow screens, contrast, touch targets, and TalkBack.
3. Complete a tagged production release only after signature and certificate fingerprint verification, installed-APK smoke tests, and owner sign-off.
4. Continue architecture work incrementally (repository boundary and lifecycle-aware state) only with migration-safe tests; avoid a broad rewrite.

**Validation boundary:** CI build/test success does not prove device behavior or production signing correctness. See RELEASE_CHECKLIST.md for release gates and WORKLOG.md for dated run evidence.


## UI/UX maturity roadmap — approved direction (2026-10-12)

This roadmap is governed by [UI_UX_RULES.md](UI_UX_RULES.md). The approved prototype direction is a focused note editor with an animated **right-side Appearance panel**; the existing bottom appearance picker is not the target interaction.

### P0 — Note creation/editor redesign (start here)
- [ ] Audit all editor modes and the create/edit entry points before changing layout.
- [ ] Rework title, body, formatting toolbar, category and primary/secondary action hierarchy.
- [ ] Replace the appearance bottom sheet with a reusable right-side slide-in panel; on phones it overlays the editor instead of permanently narrowing the writing canvas.
- [ ] Provide wallpaper/background thumbnails, colour swatches, selected indicators, live draft preview, theme controls, and predictable Apply/Cancel/Back behavior.
- [ ] Reuse the same editor and Appearance component for new and existing notes.
- [ ] Preserve save/discard prompts, stale-draft protection, pinning, reminders, archive, delete, share/copy and persistence.
- [ ] Validate keyboard/insets, narrow screens, large text, light/dark themes, and accessible labels.
- **Exit gate:** relevant automated checks and CI pass on the exact final commit; record manual/device verification separately. Do not mark complete from compilation alone.

### P1 — Whole-app UI/UX maturity audit
- [ ] Audit Home, All notes, Categories, Reminders, More, Pinned, Archive, Settings, menus, dialogs, icons, empty/error states and navigation.
- [ ] Standardize spacing, typography, shape, motion, selected/disabled states and primary-action hierarchy.
- [ ] Verify accessible contrast across themes and note appearance variants; check touch targets, focus, screen readers, text scaling and system insets.
- [ ] Remove duplicated controls/pickers and ensure every visible action works or is clearly identified as unavailable/deferred.
- **Exit gate:** shared patterns are reused consistently and a screen-by-screen audit records pass, issue, or not-tested status.

### P2 — Backend and new features on the mature UI
- Start feature expansion only after the relevant UI components and interaction contracts are stable.
- Reuse existing note models, storage boundary, navigation, design tokens, and components; do not introduce parallel UI or persistence for the same concept.
- Each feature must document its owner/state, failure/empty states, accessibility, migration needs and tests.

### Required rule
Every UI PR must follow [UI_UX_RULES.md](UI_UX_RULES.md): audit → reference → acceptance criteria → reuse-first implementation → tests → responsive/accessibility review → worklog update. P0 is the immediate priority; later feature work must not bypass it.


### P0 implementation status update — 2026-10-12
- [x] Replace the bottom appearance picker call with the reusable animated right-side panel.
- [x] Add dismissible scrim, system Back handling, colour/background previews, selected states and test tags.
- [x] Improve formatting toolbar hierarchy with a grouped tonal surface and clearer action labels.
- [ ] Audit/unify the Quick Dialog editor path so it does not become a parallel reduced-feature editor.
- [ ] Verify keyboard/insets, reduced motion, accessibility, large fonts and actual narrow/wide device layouts.
- [x] Re-run required CI on the latest branch head and record exact evidence (run `38315174209`, commit `d9d134eb72ab2bfcd5125dad69b7f3cd6c5023b2`, result: success; hosted emulator execution intentionally excluded).
- **Status:** implementation in progress; do not mark P0 complete yet.


### P0 editor audit follow-up — 2026-10-13

- [x] Remove the parallel reduced-feature Quick Dialog form and reuse `NoteEditorContent` for Quick Dialog create/edit.
- [x] Preserve Quick Dialog as a user-selectable bounded dialog layout; reuse the same appearance, category, pin/reminder, formatting, sharing/copying, delete, save and discard interactions.
- [x] Add explicit appearance Apply/Cancel draft semantics; Cancel, close, outside tap, and system Back restore the panel-opening appearance draft, while Apply keeps selections in the editor draft.
- [ ] Validate compact-dialog keyboard/insets and scroll reachability at narrow sizes and large font settings.
- [x] Confirm required CI passes on the exact branch head `fb6c096e85c55d78286e02c59aeb26f49efbcd0c` (run [#38315774563](https://github.com/dukeoftoad/toadNOTES/actions/runs/38315774563): unit tests, debug APK build, minified release compilation, and instrumentation-test compilation passed; hosted emulator execution intentionally excluded).
- [ ] Execute instrumentation tests and manually verify compact-dialog keyboard/insets, narrow screens, large fonts, and accessibility on an available local device/emulator.


### Appearance accessibility regression coverage — 2026-10-13
- [x] Extend the appearance persistence regression test to assert the selected semantics state for chosen colour and background options (`assertIsSelected`).
- [x] Required CI passed on commit `26608734f1bc53dc04b592c4542baf86619cbd8e` (run [#38386226195](https://github.com/dukeoftoad/toadNOTES/actions/runs/38386226195)): unit tests, debug APK, minified release build, and instrumentation-test compilation succeeded.
- [ ] Execute instrumentation tests on a local emulator/device; CI compilation does not prove the Compose selection assertions execute successfully.
- **P0 remains open** for keyboard/insets, narrow layouts, large-font and TalkBack/device verification.


### Responsive appearance panel — 2026-10-13
- [x] Use a 360 dp max panel width on phone-sized windows and a 480 dp cap when the editor window is at least 600 dp wide, while retaining the 88% width limit.
- [ ] Visually verify phone and tablet layouts on an available emulator/device; source-level responsive sizing is not device verification.


### Appearance palette touch targets — 2026-10-13
- [x] Set colour swatches to 48 dp touch targets and allow horizontal scrolling on narrow screens rather than compressing all seven swatches.
- [ ] Verify palette discoverability and horizontal scroll behavior on a small device with TalkBack and large font settings.


## Execution track — P0 through P3 (active as of 2026-10-13)

This sequence is cumulative: later work must not bypass earlier safety gates.

### P0 — Protect notes and finish the editor
- [ ] Keep create/edit/save/discard and stale-draft protection regression-tested.
- [ ] Cover Appearance panel Apply, Cancel, close, outside tap, and system Back rollback in instrumentation tests.
- [ ] Re-verify encrypted storage, legacy migration, backup validation/merge, and recovery behavior with unit tests.
- [ ] Close keyboard/insets, narrow-window, large-text, TalkBack, and device checks only when actually executed and recorded.
- **Current status:** editor implementation is in place; additional dismissal tests were added on the active branch. Exact-head CI and device execution remain pending.

### P1 — Audit and polish the whole app
- [ ] Add regression coverage for search across title/body/category and combined filtering/sorting.
- [ ] Review Home, All notes, Categories, Reminders, More, Pinned, Archive, Settings, menus, dialogs, and empty/error states.
- [ ] Correct contrast, touch target, selected/disabled semantics, focus order, and large-text issues found in the audit.
- [ ] Record every screen as pass, issue, or not tested; don't equate test tags/build success with accessibility verification.
- **Current status:** search regression coverage is being expanded; whole-app and device review remain open.

### P2 — Improve editing and architecture incrementally
- [ ] Improve checklist interaction/cursor preservation without introducing a parallel rich-text model.
- [ ] Extract the existing NoteStore behind a small repository boundary with tests before moving UI state.
- [ ] Consider ViewModel/Room migration only after behavior and compatibility tests demonstrate the need.
- [ ] Document migration, error, empty, and recovery states for each new feature.
- **Gate:** no broad rewrite and no storage migration without backward-compatibility and recovery tests.

### P3 — Release engineering and production proof
- [ ] Align release checklist with actual GitHub Actions secret names and release triggers.
- [ ] Verify the permanent keystore, alias, and certificate fingerprint without printing secrets or committing key material.
- [ ] Confirm manual release dispatch produces a signed APK artifact; version-tag release publishes the version-named APK.
- [ ] Verify APK signature and compare the certificate fingerprint to the approved production fingerprint.
- [ ] Install/smoke-test the downloaded artifact and record owner approval before production release.
- **Current status:** release workflow and checklist are under audit; signing and installed-APK proof must be run with the owner's configured secrets/device.


### UI/UX roadmap cross-check — 2026-10-13

This section reconciles the implementation checklists above with the latest P0–P3 execution track and current PR #10 state. It does not erase historical status entries.

- **P0 implementation:** the right-side Appearance panel, responsive width caps, 48 dp horizontally scrollable colour swatches, shared Quick Dialog/full-screen editor, Apply/Cancel draft semantics, and appearance dismissal/search regression tests are implemented on `feature/phase-7-dashboard-ux`.
- **Exact-head CI:** run [#38415822493](https://github.com/dukeoftoad/toadNOTES/actions/runs/38415822493) passed on commit `867f61f2f7f1d6baff42c8db1d3644ac126145e1`: JVM unit tests, debug APK build, minified release APK build, and instrumentation-test APK compilation passed.
- **Still open for P0:** instrumentation tests have not been executed on an emulator/device. Compact-dialog keyboard/insets and scroll reachability, narrow/wide visual review, large text, reduced motion, contrast and TalkBack remain unverified. P0 must not be marked complete until available device checks are performed and recorded.
- **P1 progress:** search regression coverage for title/body/category has been added, but combined filtering/sorting and the screen-by-screen Home/All notes/Categories/Reminders/More/Pinned/Archive/Settings audit remain incomplete.
- **P2:** defer architecture/new-feature expansion until P0/P1 UI contracts are stable; then extract a tested repository boundary without a broad rewrite.
- **P3:** release checklist now uses `KEYSTORE_BASE64`; verify the actual production certificate fingerprint, signed release publication, installation and upgrade behavior only with configured owner secrets and an available device. No production-signing proof is claimed here.
- **PR #10:** remains open pending owner approval and outstanding P0 manual/device validation. CI success means compile/build/test-task success only; it is not a device/accessibility sign-off.


### P1 implementation follow-up — 2026-10-14

- Fixed the combined filter/sort instrumentation test's unsupported Compose assertion API after CI run [#38520909916](https://github.com/dukeoftoad/toadNOTES/actions/runs/38520909916) failed during instrumentation-test compilation. The run's unit tests and debug/minified release builds passed; test APK compilation did not.
- Code audit identified a double-dispatch risk in Settings `SwitchRow`: both the parent row and nested Switch handled clicks. The row now owns the switch role/toggle semantics and the nested Switch has no independent change callback.
- **Validation pending:** exact-head CI for commits `30b1a4104a6a43ed996f8e77cc5fca1821db3fa4` and `240adb7479a53f127bb2dc76df7f7b9d9dd53941` has not yet been confirmed. Keep P0/P1 gates open until CI passes and device/accessibility validation is actually performed.


### Editor writing-space and toolbar layout correction — 2026-10-18
- [x] Move voice recording controls out of the horizontally scrolling Markdown formatting toolbar; the recorder is now a separate full-width editor section instead of being clipped beside formatting actions.
- [x] Reduce the default full-screen body field minimum from 10 to 4 lines so the writing canvas adapts better when the keyboard is visible.
- [x] Reduce excess spacing above the formatting toolbar while preserving its horizontal scrolling and existing Markdown actions.
- [ ] Verify create/edit, keyboard/insets, narrow/wide layouts, large text, TalkBack and light/dark themes on an emulator/device; no device verification is claimed by this source change.
- Implementation commit: [4e68cac](https://github.com/dukeoftoad/toadNOTES/commit/4e68cac1cd003ddad9cc89954d0a986b3b98f250).
