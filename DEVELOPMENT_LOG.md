# NESTNOTE Engineering Decisions

Last updated: 2026-10-23
Current release baseline: **1.4.5**

This file records durable engineering decisions. Implementation/validation history belongs in WORKLOG.md; planned work belongs in ROADMAP.md.

## Platform and delivery
- Use native Kotlin, Jetpack Compose, and Material 3; minimum API 26 and target/compile API 36.
- Use Java 17 and the Gradle version pinned by CI.
- Production publishing is tag-based (v*.*.*). A source commit or CI artifact is not itself a published release.
- Use only the permanent signing secrets configured for the release workflow; missing credentials must fail production publishing.

## Data safety and migration
- Encrypt local note payloads with AES-GCM and an Android Keystore-managed key.
- During plaintext migration, commit encrypted data before removing the old plaintext value. On failure, preserve original bytes and expose recovery.
- Disable Android automatic app-data backup. User-facing portability is limited to password-protected encrypted archives.
- Validate encrypted archive restores fully before mutation. Restore merges by stable note ID and never deletes local notes merely because they are absent from the archive.

## Private Vault and biometrics
- Treat Private Vault as an app-level access gate unless independent cryptographic isolation is implemented and verified.
- Biometric authentication must authorize the actual Keystore operation via an authenticated BiometricPrompt CryptoObject; prompt success alone is insufficient.
- Missing, invalidated, or corrupt biometric key material fails closed. Preserve password recovery and do not persist enabled state before enrollment succeeds.
- Private-note classification is metadata, not encryption; portable archives exclude Private Vault notes and require a separate password.

## Editor, Markdown, and zoom
- Editor and saved-note reader share presentation-only text zoom; never rewrite note content when zoom changes.
- External Markdown viewing is transient and read-only; opening a file must not silently import it into saved notes.
- External Markdown viewer zoom remains separate from editor/reader zoom.
- Reminder date/time values are metadata until scheduling and notifications are implemented and tested.

## UI and accessibility
- Preserve saved drafts and prevent stale drafts from overwriting newer saved notes.
- Keep list/checklist formatting deterministic and preserve surrounding text, indentation, line endings, and selection where applicable.
- Treat TalkBack, large fonts, narrow screens, keyboard/insets, contrast, touch targets, themes, and reduced motion as explicit validation work.

## Documentation and validation
- Current release baseline is 1.4.5; older 1.3.x entries are historical.
- Keep worklog statements factual and separate code changes from automated/manual validation and release sign-off.
- Never distribute unsigned or inspection-only APKs as production builds.