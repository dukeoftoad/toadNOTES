# NESTNOTE Engineering Decisions

Last updated: 2026-10-12

This file records durable engineering decisions. Implementation and validation history belongs in WORKLOG.md; planned work belongs in ROADMAP.md.

## Platform and delivery
- Use native Kotlin, Jetpack Compose, and Material 3; target Android API 36 and support API 26+.
- Required Android CI runs unit tests, builds the debug APK and minified release APK, and compiles instrumentation tests.
- The emulator UI workflow is manual-only because emulator boot/UI automation has been unreliable. A compiled test suite is not evidence of a device run.
- Production publishing is tag-based (v*.*.*); pushes to main do not publish or overwrite a production release.
- Production signing uses only the permanent GitHub Actions secrets SIGNING_KEY, KEY_ALIAS, KEY_STORE_PASSWORD, and KEY_PASSWORD. Missing credentials must fail the tagged release; never substitute a temporary key.

## Data safety and migration
- Encrypt local note JSON using AES-GCM and an Android Keystore-managed key.
- When migrating legacy plaintext preferences, commit encrypted data before removing the old plaintext value. If migration/decryption fails, preserve original bytes and expose recovery rather than silently seeding sample notes.
- Disable Android automatic app-data backup; clearly disclose that user-exported JSON files are plaintext.
- Validate imports fully before mutation. Versioned backups are preferred, but legacy JSON-array imports remain supported for compatibility.
- Restore merges by stable note ID. Add missing IDs, update an existing note only when the incoming edit timestamp is newer, and never delete local notes merely because they are absent from the backup.
- Reject invalid IDs, duplicate IDs, unsupported schema versions, oversized payloads, and invalid record fields before writing.

## UI and accessibility
- Preserve saved drafts and protect against stale editor drafts overwriting a newer saved note.
- Keep list/checklist formatting deterministic and preserve surrounding text, indentation, and selection where applicable.
- Treat keyboard/insets, TalkBack, large font/display scaling, narrow screens, contrast, and touch-target checks as explicit validation work, not assumed outcomes.
- Reminder values are metadata only until scheduling/notification behavior is implemented and tested.

## Review and documentation
- Use focused pull requests and update documentation with behavior/status changes.
- Keep the work log factual; separate implemented code, automated validation, manual validation, and release sign-off.
- Never distribute unsigned, inspection-only, or legacy CI-signed APKs as production builds.
