# NESTNOTE Development Guide

Last updated: 2026-10-23
Current release baseline: **1.4.5**

## Stack and prerequisites
- Kotlin, native Android, Jetpack Compose, Material 3.
- Minimum SDK 26; compile/target SDK 36.
- Java 17; Gradle 8.13 in CI.
- Android SDK Platform 36.

## Architecture and product behavior
- Compose screens and navigation live under app/src/main/java/com/nestnote/app/.
- NoteStore.kt owns persistence, encryption, migration, backup validation, and restore/merge.
- NoteText.kt contains text/list helpers and writing counters.
- Editor and saved-note reader share presentation-only zoom from 50% to 200% in 5% steps; zoom must not mutate saved text.
- External Markdown uses a separate transient read-only viewer and independent zoom preference.
- More → Power tools includes an offline Text Toolbox. Its working text is transient and must not be saved as a note without an explicit user action.
- Private Vault is an app-level access gate. Do not claim independent cryptographic isolation without architectural evidence.

## Set up and build
1. Clone the repository and open it in a recent stable Android Studio.
2. Install Android SDK Platform 36 and use Java 17.
3. Run on a compatible Android device or emulator.

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
gradle --no-daemon assembleRelease
gradle --no-daemon assembleDebugAndroidTest
```

Run connectedDebugAndroidTest when a device/emulator is available. CI compilation alone does not prove instrumentation tests executed.

## Data safety requirements
1. Never replace unreadable stored content with starter notes.
2. Commit encrypted content before removing legacy plaintext during migration.
3. Validate the complete backup before restore; preserve notes absent from the backup.
4. Preserve legacy backup compatibility unless a tested migration plan replaces it.
5. Never log or commit signing secrets, keystores, passwords, or note contents.
6. Use only password-protected encrypted `.nestnote` archives for portability; private notes and voice-note audio attachments are excluded.
7. Biometric success must authorize the cryptographic operation, not merely flip a preference. Invalidated/missing keys must fail closed while preserving password recovery.

## Documentation and release discipline
- Current release baseline is 1.4.5. Older 1.3.x entries remain historical.
- Update README, CHANGELOG, roadmap, release checklist, and worklog when implementation/status changes.
- Separate implemented code, automated validation, manual validation, and release sign-off.
- Never report tests, device checks, signing verification, or release success without exact-commit evidence.
- Do not merge or publish without repository-owner approval.

## Accessibility and release validation
Follow UI_UX_RULES.md. Verify labels, contrast, touch targets, TalkBack, large fonts, narrow screens, keyboard/insets, themes, and reduced motion. Before declaring a build production-ready, record exact-head CI, signing verification, install/upgrade, backup/restore, migration, Keystore recovery, and biometric/password-fallback results.