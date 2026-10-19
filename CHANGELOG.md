# NESTNOTE Changelog

## v1.2.8 — Upcoming

### Fixed
- Hardened `SlimRailPanel` initial drag-anchor setup to avoid reading an uninitialized anchored-draggable offset during first layout.

### Testing
- Added Compose instrumentation regression coverage for initial closed rendering, repeated open/close transitions, and custom panel/rail widths.
- Android CI passes unit tests, debug APK build, minified release APK build, and instrumentation-test compilation. Device-only gesture interruption, predictive-back cancellation, and editor integration checks remain pending.

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
