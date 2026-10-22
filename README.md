# NESTNOTE — Native Android Notes

NESTNOTE is a local-first note-taking app built with Kotlin, Jetpack Compose, and Material 3.

- **Target SDK:** Android 16 / API 36
- **Minimum SDK:** API 26
- **Storage:** encrypted local note payloads using AES-GCM and Android Keystore
- **Sync:** no account or cloud sync

## Features
- Create, edit, pin, archive, restore, search, sort, and delete notes (with undo).
- Categories, pastel note colours, editor wallpapers, light/dark themes, and configurable dashboard layout.
- Full-screen and quick editors, checklist and bullet tools, list continuation, and optional writing statistics.
- Editor text-size options, reminder metadata, and copy/share actions.
- JSON backup and restore through Android's system file picker.
- Local-data recovery safeguards and validated, versioned backups.

## License

NESTNOTE is intended to be distributed under the GNU General Public License v3.0 only (GPL-3.0-only). See [`LICENSE`](LICENSE) and the [canonical license text](https://www.gnu.org/licenses/gpl-3.0.txt). Confirm contributor/copyright-holder rights before distribution.

## Current version and release downloads
The latest published stable release is **v1.3.10**. The current v1.3.11 release record is maintained in [`CHANGELOG.md`](CHANGELOG.md) and [`docs/release-notes/v1.3.11.md`](docs/release-notes/v1.3.11.md); v1.3.11 is not a published release until its stable tag and release workflow complete.

Visit [GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases). Only download an APK attached to a versioned production release and verify its signature before installing. Historical “latest” or “CI-SIGNED” artifacts, if present, are not proof of production signing.

Production APKs are created from tags matching v*.*.* (for example, v1.2.0). The release workflow builds the minified release variant, signs it with the permanent production keystore supplied through GitHub Actions secrets, verifies the signature, and attaches the APK to the matching GitHub Release. A successful CI build alone does not establish release readiness.

### Production release steps
1. Confirm the candidate commit has passing Android CI and release checklist sign-off, including required device/recovery checks.
2. Confirm these repository Actions secrets exist without exposing their values: KEYSTORE_BASE64 (base64-encoded keystore), KEY_ALIAS, KEYSTORE_PASSWORD, and KEY_PASSWORD.
3. Create and push an approved version tag, e.g. git tag v1.2.0 && git push origin v1.2.0.
4. Wait for [Android Release](https://github.com/dukeoftoad/toadNOTES/actions/workflows/android-release.yml) to finish.
5. Independently compare the APK certificate fingerprint with the recorded production certificate, install the exact published APK, and record the result before distribution.

A manually dispatched/non-tag release workflow run is for unsigned inspection output only; it must not be distributed as a production APK. Never commit keystores, passwords, signing keys, or other secret values.

## Data, backups, and recovery
Local notes use AES-256-GCM with a 96-bit random nonce and 128-bit authentication tag; the key is generated and held by Android Keystore. The current v2 storage format authenticates the app/storage purpose as additional authenticated data and uses a dedicated v2 key alias. Existing v1 encrypted records and legacy plaintext SharedPreferences records are migrated to v2 only after the new ciphertext is committed; the old record is removed in that same successful preferences commit. Failed decryption or authentication preserves the stored bytes and requires recovery rather than silently replacing notes. If stored content cannot be read or decrypted, NESTNOTE preserves the original data and exposes a recovery path rather than silently replacing it with starter notes.

Backups exported by the user are **plaintext JSON** and should be stored securely. **Voice-note audio attachments are intentionally excluded from backup and restore**; a restored note will not contain its recordings. This is the v1 policy, not a temporary claim that audio is protected by the JSON export. Do not copy local attachment ciphertext into backups: it is tied to this installation's key and attachment identity. Automatic Android app-data backup is disabled. Restore validates the entire file before changing notes, supports the current versioned backup envelope and legacy JSON-array backups, merges by note ID, keeps the newer edited copy when timestamps differ, and does not delete notes absent from the backup.

Encryption protects data at rest; it is not a guarantee against a compromised device while the app is running, screenshots, clipboard sharing, or exported backups. Losing/resetting the Android Keystore key may prevent decryption. Do not clear app data or uninstall before exporting a secure backup if key/device recovery is a concern. Verify key-loss recovery on a test device before release.

## Build and test
Open the repository in a recent stable Android Studio, allow Gradle sync, and install Android SDK Platform 36 if prompted.

With Java 17, Gradle 8.13, and Android SDK API 36 installed:

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
gradle --no-daemon assembleRelease
gradle --no-daemon assembleDebugAndroidTest
```

The [Android CI workflow](https://github.com/dukeoftoad/toadNOTES/blob/main/.github/workflows/android-ci.yml) runs unit tests, builds debug and minified release APKs, and compiles instrumentation tests. Emulator execution is intentionally excluded from required CI to avoid hosted-emulator subscription/usage costs. Run `gradle --no-daemon connectedDebugAndroidTest` locally on an available emulator/device when possible. Do not claim instrumentation execution, physical-device, TalkBack, large-font, keyboard/insets, upgrade, migration, or visual-regression checks unless they were actually run and recorded.

## Repository guide
- app/src/main/java/com/nestnote/app/MainActivity.kt — Compose UI and interactions.
- app/src/main/java/com/nestnote/app/NoteStore.kt — note model, persistence, encryption, backup validation, and merge.
- app/src/main/java/com/nestnote/app/NoteText.kt — list/checklist helpers and text counters.
- app/src/test/ — JVM/unit tests.
- app/src/androidTest/ — Android instrumentation tests.
- .github/workflows/ — CI, release, and manual UI-test workflows.
- [DEVELOPMENT.md](DEVELOPMENT.md) — setup and contribution guide.
- [ROADMAP.md](ROADMAP.md) — product phases and status.
- [RELEASE_CHECKLIST.md](RELEASE_CHECKLIST.md) — validation and release gates.
- [WORKLOG.md](WORKLOG.md) — implementation/CI history.
- [DEVELOPMENT_LOG.md](DEVELOPMENT_LOG.md) — engineering decisions.

## Current limitations
- Reminder dates/times are metadata only; alarm scheduling and notification delivery are not implemented.
- Checklist items can be toggled in the editor; rich-text formatting is not implemented.
- No account, remote sync, or server-side note storage is included.
- Android instrumentation tests are compiled in CI but are not automatically executed on a hosted emulator; device execution and manual accessibility/visual checks remain separate release gates.


## Mandatory UI/UX direction
NESTNOTE follows [UI_UX_RULES.md](UI_UX_RULES.md). The immediate P0 is a polished note editor with an animated right-side Appearance panel (replacing the bottom appearance picker), shared by create/edit flows. App-wide consistency and accessibility auditing follow before backend/new-feature expansion. See [ROADMAP.md](ROADMAP.md) for phases and [WORKLOG.md](WORKLOG.md) for evidence and implementation status.
