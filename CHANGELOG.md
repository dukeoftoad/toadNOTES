# NESTNOTE Changelog

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
