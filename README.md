# NESTNOTE — Android

Native Android notes app built with Kotlin, Jetpack Compose and Material 3. Target SDK: Android 16 (API 36).

## Open in Android Studio
1. Open this repository as a project in Android Studio (recent stable version).
2. Allow Gradle to sync and install the Android SDK Platform 36 if prompted.
3. Run the `app` configuration on an Android 16 emulator or device.

## Features
- Create, edit, pin, archive, restore, and delete notes; undo accidental deletion
- Search notes, filter by category, and sort by edited date, creation date, or title
- Category labels, note colors, and selectable editor backgrounds/wallpapers
- Quick capture plus full-screen and sheet editor modes
- Checklist and bullet-list tools with list continuation
- Optional word, character, and estimated reading-time counters
- Configurable editor text size and grouped settings for theme, layout, density, sorting, and default category
- Reminder date/time metadata with preset choices
- Copy and share note text
- JSON backup and restore through the Android system file picker
- Light/dark theme, dynamic color, and responsive edge-to-edge Compose layout
- Local note persistence encrypted with AES-GCM using an Android Keystore key

## Backup and restore
Use **Settings → Back up notes** to save a NESTNOTE JSON backup to a location you choose. Use **Restore backup** to merge a backup into the current notes. Notes are matched by ID; missing notes are added, and the newer edited version wins when the same ID exists in both. Restore does not delete existing local notes. Only valid NESTNOTE backup JSON is accepted. Exported JSON is plaintext and should be stored somewhere you trust. Automatic Android app-data backup is disabled.

## Local data protection

NESTNOTE encrypts local note JSON with AES-GCM using a key held by Android Keystore. Existing plaintext SharedPreferences notes are migrated on load: the encrypted payload is committed before the legacy plaintext key is removed. If notes cannot be decrypted or parsed, the app preserves the stored data and presents a recovery path rather than silently replacing it with starter notes. Exported JSON backups are plaintext. Automatic Android app-data backup is disabled.

Release builds use R8 minification and resource shrinking. CI does not sign test/debug APKs with disposable keys. Non-tag release builds are uploaded unsigned for inspection only. Production tags (v*.*.*) require the permanent Actions secrets SIGNING_KEY (base64-encoded keystore), KEY_ALIAS, KEY_STORE_PASSWORD, and KEY_PASSWORD; the workflow fails closed if any are missing and verifies the APK signature before publishing. Never distribute an unsigned or test artifact as a production update.

## Download and install APK releases

Open the [NESTNOTE GitHub Releases page](https://github.com/dukeoftoad/toadNOTES/releases) to download a published APK. Production APKs are published only from version tags such as `v1.0.0`; the release workflow builds the minified release variant, signs it with the permanent production keystore, verifies the APK signature, and attaches `NESTNOTE-vX.Y.Z.apk` to the GitHub Release.

To publish a production release after approval:

1. Confirm the candidate commit has passing Android CI and required device/recovery checks are signed off.
2. Confirm repository Actions secrets `SIGNING_KEY` (base64-encoded keystore), `KEY_ALIAS`, `KEY_STORE_PASSWORD`, and `KEY_PASSWORD` are configured. Never put secret values in the repository or logs.
3. Create and push a version tag, for example `git tag v1.0.0 && git push origin v1.0.0`.
4. Wait for **Android Release** to finish successfully, then verify the APK signature and certificate fingerprint against the independently recorded production certificate before distributing the file.

Unsigned APKs from non-tag/manual inspection builds are not production install packages. Do not use them for public distribution or upgrades.

## Project structure
- `app/src/main/java/com/nestnote/app/MainActivity.kt`: UI and interactions
- `app/src/main/java/com/nestnote/app/NoteStore.kt`: local persistence, note model, and backup encode/decode/merge
- `app/src/main/java/com/nestnote/app/NoteText.kt`: editor list helpers and text counters
- `app/src/test/`: unit tests
- `app/src/androidTest/`: Compose instrumentation tests
- `app/build.gradle.kts`: Android app configuration
- `build.gradle.kts`, `settings.gradle.kts`: Gradle project

## Current scope and limitations
- Reminder times are stored as note metadata only; alarm scheduling and notification delivery are not implemented.
- Checklist items are toggled through the editor toolbar; tapping the checkbox character in the note body does not toggle it.
- Text formatting is limited to checklist and bullet prefixes; rich-text formatting is not implemented.
- No account or cloud sync is included. Notes and preferences remain local to the device.
- Android CI builds the app, runs unit tests, builds the minified release variant, and compiles instrumentation tests; it does not sign test APKs. Instrumentation tests have not been claimed as executed on an emulator or physical device; manual UI, accessibility, larger-font, keyboard, and edge-to-edge regression checks remain follow-up validation.

## Build and test
Android Studio: sync Gradle, then Run.

Command line with Android SDK and Gradle installed:
```sh
gradle assembleDebug
gradle testDebugUnitTest
```

See [ROADMAP.md](ROADMAP.md) for phase status and [WORKLOG.md](WORKLOG.md) for implementation and validation history.
