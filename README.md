# NESTNOTE

NESTNOTE is a local-first Android notes app built with Kotlin, Jetpack Compose, and Material 3. It focuses on quick note capture, flexible editing and reading, offline use, and password-protected encrypted archives.

## Current release: 1.4.9

Version **1.4.9** is the current release candidate baseline; publish only after the exact tagged commit passes release checks. Consult [GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases) for authoritative release assets. CI artifacts are for testing unless explicitly attached to a versioned production release and verified.

## Platform
- Minimum Android: API 26; compile/target SDK: API 36.
- Kotlin, Jetpack Compose, Material 3; Java 17 build environment.
- Local-first storage with no account or cloud sync.
- Local note payload encryption uses AES-GCM and Android Keystore-managed key material.
- Settings offers two independently selectable local stores: the existing Keystore-backed encrypted payload and a SQLCipher-encrypted database in app-private storage. The SQLCipher passphrase is wrapped by a non-exportable Android Keystore key.
- Switching storage preserves both stores. The SQLCipher store is initialized from the currently loaded notes only when first created; later switches reopen each backend's own saved data. A storage/key failure blocks the switch rather than falling back to plaintext.

## Features
### Notes and writing
- Create, edit, pin, archive, restore, search, sort, and delete notes with undo.
- Categories, note colours, editor backgrounds, light/dark themes, and configurable Home layout.
- Quick and full-screen editors, checklist and bullet tools, list continuation, and writing statistics.
- Editor and saved-note reader text zoom from 50% to 200% in 5% steps. Zoom changes presentation, not saved content.
- Reminder date/time metadata and copy/share actions. Reminder metadata does not schedule notifications.

### Markdown and Power tools
- Open external Markdown files in a transient read-only viewer without importing them into saved notes.
- External Markdown viewer zoom from 50% to 200%, with a configurable default.
- Offline Text Toolbox under More → Power tools: word/character/line counts, estimated reading time, case conversion, whitespace cleanup, copy, and clear.

### Backup and privacy
- Password-protected `.nestnote` archives use authenticated AES-GCM encryption and are stored under `Documents/NESTNOTE` after folder access is granted.
- Private Vault access from More or Settings → Privacy, with password protection and biometric enrollment/unlock via an authentication-bound Android Keystore key and BiometricPrompt CryptoObject; physical-device validation remains a release gate.
- Private Vault is an app-level access gate, not a guarantee of independent cryptographic isolation.
- Portable archives exclude Private Vault notes and voice-note audio attachments.

## Security boundaries
Local encryption at rest does not protect content on a compromised device while the app is running or through clipboard sharing. Private-note classification is metadata, not encryption. Portable archives are separately password-protected and exclude Private Vault notes. Keystore loss may prevent local decryption; understand the archive limitations before uninstalling or clearing app data.

Biometric authentication must authorize the actual cryptographic operation through an authenticated BiometricPrompt CryptoObject. Missing, invalidated, or corrupt biometric key material must fail closed while password recovery remains available. See [Security Model](docs/SECURITY.md) and [Threat Model](docs/THREAT_MODEL.md).

## Build and test
Install Android SDK Platform 36 and use Java 17.

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
gradle --no-daemon assembleRelease
gradle --no-daemon assembleDebugAndroidTest
```

The Android CI workflow builds APK variants and runs configured checks. Compiling instrumentation tests is not proof they ran on a device; use connectedDebugAndroidTest on an available emulator/device and record results for the exact commit.

### Downloading builds
Use [GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases) for versioned production APKs. The nestnote-debug-apk Actions artifact, when available, is for testing and is not a production release. Verify the production signing certificate before distribution. Never commit signing keys or secrets.

## Repository guide
- app/src/main/java/com/nestnote/app/ — Compose screens, navigation, editor, vault, and tools.
- NoteStore.kt and SqlCipherNoteStore.kt — selectable encrypted persistence backends, encryption, and recovery.
- PasswordBackupArchive.kt — password-protected encrypted backup archives.
- NoteText.kt — text/list helpers and writing statistics.
- app/src/test/ — JVM tests; app/src/androidTest/ — Android instrumentation tests.
- .github/workflows/ — CI, release, and UI-test workflows.
- [DEVELOPMENT.md](DEVELOPMENT.md) — setup and contribution guide.
- [CHANGELOG.md](CHANGELOG.md) — version history.
- [ROADMAP.md](ROADMAP.md) — planned work and status.
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) — release gates.
- [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) — engineering decisions.
- [WORKLOG.md](WORKLOG.md) — implementation and validation history.
- [UI_UX_RULES.md](UI_UX_RULES.md) — UI and accessibility principles.

## Known limitations
- Reminder fields are metadata only; scheduling and notifications are not implemented.
- Rich-text editing and cloud sync are not included.
- Portable archives are password-protected and encrypted; private notes and audio attachments are excluded.
- Device migration/key-loss recovery, biometric edge cases, TalkBack, large-font/narrow-screen, and keyboard/insets checks must be recorded separately from automated builds.

## License
Intended license: GPL-3.0-only. See [LICENSE](LICENSE) and the [canonical license text](https://www.gnu.org/licenses/gpl-3.0.txt). Confirm contributor and copyright-holder rights before distribution.

- Markdown notes support formatted tables in preview; pasting a valid pipe table into the editor automatically aligns columns while preserving cell text and alignment markers.
