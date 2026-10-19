# NESTNOTE Engineering Decisions

Last updated: 2026-10-19

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


## UI/UX design decision — 2026-10-12
- **Decision:** treat UI/UX maturity as a product gate, not as a per-screen polish task.
- **Approved editor direction:** a focused note editor with an animated right-side Appearance panel instead of a bottom colour-picker sheet. The panel overlays the editor on phones, includes wallpaper previews, colour swatches, selected indicators, live draft preview, and clear dismissal/apply behavior.
- **Reuse rule:** one shared editor for create/edit, one shared Appearance component, one note save/persistence path, and consistent design tokens. No duplicate UI or parallel state/persistence paths.
- **Sequencing:** P0 editor redesign; P1 whole-app screen/menu/icon/accessibility audit; P2 backend and new features on the mature reusable UI.
- **Source finding:** `NoteEditorContent` now uses the shared `EditorToolRail` for Colour, Background and Format. The rail owns draft Apply/Cancel behavior and is reused for create and edit flows.
- **Remaining gate:** validate the rail, editor and keyboard/insets behavior on a device, then measure cold startup and note editing before calling the redesign complete. The parity checklist is tracked in [docs/PROTOTYPE_PARITY_AUDIT.md](docs/PROTOTYPE_PARITY_AUDIT.md).
- **Validation boundary:** require CI for the exact final commit and separately record manual/device visual, keyboard/insets, large-font, contrast and TalkBack checks. Never infer these from a successful compile.
- Governing contract: [UI_UX_RULES.md](UI_UX_RULES.md).
