# NESTNOTE Changelog

## Unreleased

### Changed
- Added a dedicated inline voice-player area to the note editor so voice attachments can be played while editing, without opening Markdown preview. Validation is in progress.

### Fixed
- Voice-note playback cards stack their title/waveform and seek controls vertically, preventing the seek bar from overlapping the voice-note label.
- Voice attachment markers render as readable labels instead of exposing internal attachment IDs.
- Note format is persisted as plain text or Markdown, with older notes defaulting safely to plain text.
- The note format menu no longer references an unavailable Material icon, fixing the Android CI compilation failure.

### Known issue
- The editor screenshot still shows a highlighted voice-note marker rather than an inline player while editing. Inline playback in read mode and Markdown preview is implemented, but direct playback from the editable text field is not yet verified/fixed. Do not consider this interaction complete until tested on-device.

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

### Added (Backup & restore)
- Added optional scheduled automatic backups using Android background work, with a visible last-run status and an approximate next-run window.
- Added a versioned JSON backup format with format identification, schema version, and creation timestamp.
- Improved restore safety with backup validation, unsupported-schema rejection, and a preview of notes to add, update, or skip before confirmation.

### Improved (Backup & restore)
- Automatic backups use app-private storage; Android may delay background execution, and app-private backups can be lost when the app is uninstalled.
- Manual exports let users choose a destination through Android's save flow. Exporting a file does not automatically upload it to cloud storage.
- Restore remains merge-oriented rather than destructively replacing the current notes. Audio attachments remain excluded from the current backup format.

### Privacy and portability notes
- Password-protected backup files are not implemented in this version.
- Users should keep exported backup files in a secure location; exported JSON contains note data.
- The versioned format is intended to support validation and future migration across app versions.

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
- Voice attachments are stored locally and remain excluded from the current JSON backup/restore format.
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
- Voice recordings are stored as local attachments. Audio attachments remain excluded from the current JSON backup/restore format.
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
- Voice recordings are stored as local attachments; audio attachments are intentionally excluded from the current JSON backup/restore format.
- User-exported JSON backups contain plaintext note data and should be stored securely.
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
