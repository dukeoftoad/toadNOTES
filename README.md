# NESTNOTE — Android

Native Android notes app built with Kotlin, Jetpack Compose and Material 3. Target SDK: Android 16 (API 36).

## Open in Android Studio
1. Open this repository as a project in Android Studio (recent stable version).
2. Allow Gradle to sync and install the Android SDK Platform 36 if prompted.
3. Run the `app` configuration on an Android 16 emulator or device.

## Features
- Compose dashboard inspired by the supplied NoteNest reference
- Create, edit, pin, and delete notes
- Search notes and filter by category
- Category labels and note colors
- Quick capture
- Reminder date/time metadata
- Light/dark theme toggle
- Local persistence using SharedPreferences
- Edge-to-edge, responsive Compose layout

## Project structure
- `app/src/main/java/com/nestnote/app/MainActivity.kt`: UI and interactions
- `app/src/main/java/com/nestnote/app/NoteStore.kt`: local persistence and note model
- `app/build.gradle.kts`: Android app configuration
- `build.gradle.kts`, `settings.gradle.kts`: Gradle project

## Current scope
This is a native MVP. Reminder times are saved locally; exact alarms and notification delivery are not yet implemented. No account/cloud sync is included yet. User data remains on-device.

## Build
Android Studio: sync Gradle, then Run.
Command line with Android SDK and Gradle installed: `gradle assembleDebug`.
