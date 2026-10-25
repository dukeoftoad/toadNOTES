# NESTNOTE Changelog

## Unreleased — encrypted-only portability

- Removed plaintext JSON backup/export and import controls and their file-handling paths.
- Removed scheduled plaintext JSON backups and their WorkManager worker/scheduler.
- Kept password-protected AES-GCM `.nestnote` archives as the only user-facing note portability option.
- Archive payload serialization remains an internal implementation detail; Private Vault notes and voice attachments are excluded.


## v1.4.6 — Release candidate

- Align app release metadata and current documentation with version 1.4.6.
- Carry forward native Markdown table rendering and pipe-table alignment.
- Include the Private Vault strong-biometric CryptoObject path; device validation remains a release gate.
- Bound SAF backup verification reads to the expected payload size.
- Do not treat this entry as proof of publication, signing, or exact-head CI success.

## v1.3.11 — Historical repository release record (2026-10-22)

Versioned notes: [docs/release-notes/v1.3.11.md](release-notes/v1.3.11.md).

This is a historical release record retained for traceability. The current candidate is v1.4.6; publishing it still requires a stable tag and successful release workflow.

### Fixed
- Improve editor foreground contrast on dark surfaces using surface luminance and theme-provided colors.
- Improve Markdown blockquote and horizontal-rule rendering while keeping raw HTML inert.
- Preserve whitespace, indentation, trailing-newline behavior, and LF/CRLF/lone-CR line endings during text conversion and list operations.
- Improve selection remapping after line edits and harden inline formatting and checklist/bullet transitions.
- Restrict actionable Markdown links to `http`, `https`, and `mailto` schemes.
- Improve accessibility touch targets and standalone Markdown heading semantics.

### Markdown renderer and link insertion — PR #74 merged 2026-10-22
- Replaced the regex-based preview preprocessor with Markwon/CommonMark and added task-list and strikethrough extensions.
- Hardened safe link-label escaping and cursor placement, with Unicode, punctuation, selection, and boundary regression cases.
- PR #74 merged as `479d6a2013a18a6673f179c00e147c24e4ed798f`. Its merge-triggered Android CI run is still in progress; do not call the release validated until it completes successfully.

### External Markdown viewer and zoom — PR #87 (merged 2026-10-23)
- Add direct read-only opening of external `.md` files without creating/importing a saved note.
- Provide viewer zoom from 50% to 200% in 5% increments, with a vertical control group under the viewer's More menu and a restore-default action.
- Add a persisted default zoom setting under Settings → Note editor; retain the existing file-import workflow separately.
- PR #87 merged on 2026-10-23 as `c7954ae8d9f815c0040595805ac50b9c36e880da`. The feature is part of the v1.3.11 candidate record; v1.3.11 is not published until its stable tag and release workflow complete.

### Validation status
- Confirm exact-head Android CI before declaring the release validated.
- Production APK signing, installation/upgrade checks, and physical-device accessibility checks are not claimed complete by this documentation update.

---

## v1.3.10 — 2026-10-21 (published release)

- Fixed GitHub Pages preservation during F-Droid publishing.
- Showed the voice-note waveform in the in-place note editor.
- Release asset: [NESTNOTE-v1.3.10.apk](https://github.com/dukeoftoad/toadNOTES/releases/download/v1.3.10/NESTNOTE-v1.3.10.apk).

---


## v1.3.9 — 2026-10-21 (published release; historical entry)

Versioned notes: [docs/release-notes/v1.3.9.md](release-notes/v1.3.9.md).

This section is retained as the historical v1.3.9 release record. Later fixes and release changes are tracked in the newer version sections above.

### Improved
- Keep voice-note playback inline with the note instead of opening a separate player or covering the page.
- Make voice-note cards easier to identify and operate, with recording labels, waveform/player controls, and clearer accessibility descriptions.
- Improve editor handling of voice-note markers so internal attachment identifiers are not exposed as ordinary note text.
- Preserve the existing note text and note-format behavior while working with voice attachments.

### Fixed
- Correct voice-player layout so labels, waveform, and seek controls do not overlap.
- Fix note-format menu compilation by removing a reference to an unavailable Material icon.
- Persist plain-text versus Markdown note format and safely default older notes to plain text.
- Add regression coverage for inline/adjacent voice markers, text preservation, and marker removal.

### Scope note (historical)
- Encrypted unique-link sharing and QR-code sharing were not part of the v1.3.9 release record.
- Wallpaper-import availability must be checked against the current implementation and tests; this historical entry does not claim it shipped in v1.3.9.

---

## Historical voice-player implementation notes — superseded by v1.3.10

The following entries preserve earlier implementation notes for traceability. The current published release scope is recorded under v1.3.10 above; this is not a separate unreleased release.

### Changed
- Added a dedicated inline voice-player area to the note editor so voice attachments can be played while editing, without opening Markdown preview.

### Fixed
- Voice-note playback cards stack their title/waveform and seek controls vertically, preventing the seek bar from overlapping the voice-note label.
- Inline voice-player play/pause controls include the recording name in their accessible labels, and the seek slider exposes its current position and total duration.
- Added Compose instrumentation assertions for accessible names on missing inline voice-attachment cards.
- Voice attachment markers render as readable labels instead of exposing internal attachment IDs.
- Note format is persisted as plain text or Markdown, with older notes defaulting safely to plain text.
- The note format menu no longer references an unavailable Material icon, fixing the Android CI compilation failure.

### Historical validation note
- Earlier editor screenshots showed a highlighted voice-note marker rather than an inline player while editing. v1.3.10 subsequently records the in-place voice-note waveform fix; this historical note alone does not certify device-level validation.

### Tests
- Added regression coverage for inline and adjacent voice markers, text preservation, and marker removal.



---

## 3. Release notes for `v1.3.1`

Create `CHANGELOG.md` entry or append to the top (match your existing format — you have a `CHANGELOG.md` in the tree):

```markdown
## v1.3.1 — 2026-10-20

Bug-fix release. No new features, no data migration. Safe drop-in upgrade from
v1.3.0.

### Fixed

- **Draft recovery reliability** — In-progress drafts are now serialized with a
  format that survives both on-device and in unit tests. Fixes a bug where the
  draft store could fail silently on certain note sizes, leaving crash recovery
  unreliable.
- **Voice note state preserved across UI updates** — When editing an existing
  note, voice recordings made during the session are no longer lost if the app
  is backgrounded, rotated, or a snackbar appears. Previously the editor could
  silently discard pending voice state under specific recomposition sequences.
- **Orphan draft recovery survives rotation** — If the app was killed while
  editing a brand-new note, the "Recover unsaved note?" dialog now re-appears
  correctly after device rotation. Before, the dialog could be dismissed
  implicitly when the screen rotated mid-recovery.
- **Reminder scheduling moved off the main thread** — Setting or removing a
  reminder on a device with many reminder-enabled notes no longer causes a brief
  UI pause. Alarm scheduling now happens on a background dispatcher.

### Internal

- Replaced `org.json` with `java.util.Properties` in `DraftStore`. Eliminates
  the JVM/Android classpath asymmetry that was blocking unit tests.
- Test suite: 207 tests, all passing (was 201/207).

### Known issues

See [docs/KNOWN_ISSUES.md](docs/KNOWN_ISSUES.md) for the current list.
Remaining open items are all low-priority UX gaps.


## v1.3.2

### Fixed (Android CI)
- Show the inline voice-note seek slider for short recordings as well as long clips; previously, seeking was hidden for every clip shorter than 60 seconds.
- Run Android lint, unit tests, debug/release APK builds, and instrumentation-test compilation through the repository Gradle wrapper to keep the Gradle version consistent with the project.
- Remove hardcoded local Java 17 paths from `gradle.properties` so CI uses the Java installation configured on the runner.
- Hardened `SlimRailPanel` initial drag-anchor setup to avoid reading an uninitialized anchored-draggable offset during first layout.

### Historical feature (removed in 1.4.7)
- Earlier versions offered scheduled automatic backups and versioned JSON export/import. These plaintext portability paths have been removed; encrypted `.nestnote` archives are the only current user-facing portable backup option.

### Current encrypted archive behavior
- Archives are written to the user-selected Documents/NESTNOTE folder after folder access is granted.
- Restore is merge-oriented rather than destructively replacing current notes. Private Vault notes and voice attachments are excluded.

### Privacy and portability notes
- Current portable archives are password-protected and AES-GCM encrypted; plaintext JSON export/import is not offered.
- Historical note: the retired JSON export format contained readable note data. Current portable backups use password-protected encrypted `.nestnote` archives.
- Internal archive payload schema checks support validation and compatibility across app versions.

### Testing and release readiness
- Add Robolectric regression tests for plaintext and encrypted-v1 note migrations, migration idempotence, corrupt/tampered payload recovery, and preservation of unreadable source data.
- Add Compose settings regressions for search filtering and fallback from an obsolete persisted sort value.
- CI configuration fixes address Gradle provisioning and Java-home failures; confirm a complete green workflow run before release.
- Device-only gesture interruption, predictive-back cancellation, and editor integration checks remain pending.

## v1.3.1 — Current release

### Added
- Expanded the Markdown editor toolbar with H2/H3 headings, configurable heading levels, numbered lists, links, blockquotes, strikethrough, and inline code.
- Added Material 3 personalization and more flexible note creation.
- Added encrypted local voice-attachment storage, attachment metadata, and voice-note recording/playback integration.
- Added inline voice markers and preview-mode playback, with a single-player coordinator and insertion at the editor cursor.

### Improved
- Redesigned the app around a floating four-tab navigation bar with a central New action and a More hub for Library, Data, Preferences, Settings, Pinned, Archived, and Categories.
- Refreshed the visual system with Fraunces display typography, warm light/dark palettes, an optional AMOLED-black theme, and shared expressive motion.
- Reworked the editor with a slim right-side tool rail, compact Markdown controls, borderless writing fields, keyboard-docked formatting actions, lock/edit modes, and on-demand voice controls.
- Improved adaptive layouts, predictive-back navigation, and editor sizing for expanded windows.
- Hardened voice-attachment lifecycle handling, including safe cleanup after deletion is committed and orphan-attachment reconciliation.
- Updated F-Droid listing/metadata documentation to reflect current app features and added checks for repository publication.

### Fixed
- Fixed the `SlimRailPanel` initial anchored-drag offset crash and added Compose regression coverage.
- Corrected Markdown numbered-list matching, link insertion cursor placement, and Markdown preview link navigation.
- Added Markdown preview HTML-safety tests and expanded editor formatting regression coverage.

### Notes
- Voice attachments are stored locally and were excluded from the legacy JSON backup format; plaintext JSON backup/restore has since been removed.
- The self-hosted F-Droid repository is separate from the official F-Droid listing; successful workflow completion should be followed by checking the published repository index.
- This entry summarizes feature and fix commits found in the repository history. The exact v1.3.1 release date and precise commit/tag boundary were not independently confirmed.

## v1.2.8 — Historical entry

### Changed (UI/UX redesign, branch `feature/ui-redesign`)
- Split `MainActivity.kt` into theme, preferences, app shell, settings, note card, editor and appearance files (no behaviour change).
- New warm light/dark palettes, Fraunces display font (SIL OFL, see `docs/THIRD_PARTY_FONTS.md`) and shared motion tokens; fixed the typography definition dropping default font sizes.
- Floating four-tab bar (Home, Notes, New, Reminders, More); the More tab now hosts Pinned, Archived, Categories, Backup & restore and Settings.
- Editor: slim vertical tool rail (Colour, Background, Format) replaces the two wide side panels; pin, reminder, voice note, read mode, archive, share, copy and delete moved into the editor More menu; single category chip; voice recorder revealed on demand.
- Optional pure-black dark mode.

### Fixed
- Hardened `SlimRailPanel` initial drag-anchor setup to avoid reading an uninitialized anchored-draggable offset during first layout.
- Updated Android CI to run all build and test tasks through the repository's Gradle wrapper, keeping the Gradle version consistent with the project configuration.
- Removed hardcoded local Java 17 paths from `gradle.properties` so CI uses the Java installation configured by the runner.

### Testing
- Added Compose instrumentation regression coverage for initial closed rendering, repeated open/close transitions, and custom panel/rail widths.
- Android CI fixes address Gradle provisioning and Java-home configuration failures. A successful full CI run after these fixes still needs confirmation; device-only gesture interruption, predictive-back cancellation, and editor integration checks remain pending.

## v1.2.7

### Added
- Redesigned note-editor appearance and formatting access with a slim rail and more discoverable controls.
- Expanded note appearance customization and integrated Markdown formatting tools, including headings, bold, italic, strikethrough, inline code, links, lists, checklists, and blockquotes.
- Release-to-F-Droid workflow orchestration so the self-hosted F-Droid publishing workflow can run after the GitHub release workflow completes successfully.

### Improved
- Refined the editor layout and interaction patterns using stable Material 3 components and API 36-compatible implementation choices.
- Improved access to formatting and appearance controls while keeping the writing area focused.
- Clarified release automation sequencing to reduce the chance of publishing F-Droid metadata before a successful release build.

### Distribution notes
- The self-hosted F-Droid repository remains separate from the official F-Droid listing.
- Automated workflow completion does not by itself prove that the repository index is available; verify the published index after a successful publishing run.

## v1.2.6

### Added
- Inline voice recording and audio-file attachment actions inside the Note editor.
- A recording review step after stopping, with preview playback, rename, explicit Attach, and Discard actions.
- Inline audio attachment cards with play/pause, seek/progress, elapsed time, and total duration.
- Support for multiple audio attachments in one note, with independent controls for each card.

### Improved
- Kept audio recording and playback within the Note tab, without playback dialogs, a separate audio tab, a mini-tab, or a floating mini-player.
- Improved recording-state feedback, elapsed-time display, and the review-before-attach workflow.
- Preserved existing note content and attachments when reviewing or discarding an additional recording.
- Continued hardening audio attachment lifecycle and playback regression coverage.

### Compatibility and privacy notes
- Audio-file import currently supports uncompressed 16-bit mono or stereo WAV files; other formats are not yet supported.
- Voice recordings are stored as local attachments. Audio attachments are excluded from portable `.nestnote` archives.
- NESTNOTE remains local-first with no account-based cloud sync.

## v1.2.5 — 2026-10-18

### Added
- Voice-note recording and playback support, including audio attachment metadata and encrypted local attachment storage.
- Voice attachment lifecycle cleanup, including cleanup after note deletion and the undo window.
- Contextual Markdown heading control in the editor.
- NESTNOTE branding assets for web and F-Droid repository presentation.

### Improved
- Hardened local note encryption with authenticated v2 storage.
- Improved saved voice-recording playback handling and regression coverage.
- Strengthened recording recovery and lifecycle behavior.
- Expanded editor, storage, backup, release, and F-Droid distribution documentation.

### Privacy and backup notes
- Voice recordings are stored as local attachments; audio attachments were excluded from the legacy JSON backup/restore format, which is no longer a user-facing option.
- Historical note: older versions offered user-exported plaintext JSON files. JSON export/import is removed; use password-protected encrypted `.nestnote` archives.
- NESTNOTE remains local-first with no account-based cloud sync.

### Distribution
- GitHub Release APK: https://github.com/dukeoftoad/toadNOTES/releases/tag/v1.2.5
- Self-hosted F-Droid repository publication is managed separately and should be considered available only after its publishing workflow succeeds and the repository index is verified.
- This self-hosted repository is not an official F-Droid listing.

### Changes included
- CI and signed Android release workflow improvements
- F-Droid repository automation and branding
- Authenticated v2 note storage and voice attachment foundation/lifecycle
- Voice recording/playback regression fixes and recovery documentation
- Editor and backup/release audit updates

Full comparison: https://github.com/dukeoftoad/toadNOTES/compare/v1.2.4...v1.2.5

## Candidate additions in PR #94 — not yet published
- Home dashboard quick-create card with active and pinned note counts.
- Offline Text Toolbox under More → Power tools (text statistics, reading-time estimate, case transforms, whitespace cleanup, copy, and clear).
- Expandable note editor/reader zoom slider (50%–200%, 5% increments); display-only and does not change stored text.
- Private Vault biometric enrollment/unlock flow and modernized settings/dialog UI remain subject to exact-head CI and device-level verification before release.
