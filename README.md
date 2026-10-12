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
- Local persistence using SharedPreferences

## Backup and restore
Use **Settings → Back up notes** to save a NESTNOTE JSON backup to a location you choose. Use **Restore backup** to merge a backup into the current notes. Notes are matched by ID; missing notes are added, and the newer edited version wins when the same ID exists in both. Restore does not delete existing local notes. Only valid NESTNOTE backup JSON is accepted.

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
- Android CI builds the app, runs unit tests, and compiles/signs the instrumentation test APK. Instrumentation tests have not been claimed as executed on an emulator or physical device; manual UI, accessibility, larger-font, keyboard, and edge-to-edge regression checks remain follow-up validation.

## Build and test
Android Studio: sync Gradle, then Run.

Command line with Android SDK and Gradle installed:
```sh
gradle assembleDebug
gradle testDebugUnitTest
```

See [ROADMAP.md](ROADMAP.md) for phase status and [WORKLOG.md](WORKLOG.md) for implementation and validation history.
