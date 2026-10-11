# NESTNOTE Development Guide

Last updated: 2026-10-11

## Technology
- Kotlin, native Android, Jetpack Compose, Material 3.
- Target SDK Android 16 / API 36; minimum SDK 26.
- Local note persistence through the existing NoteStore / SharedPreferences implementation.

## Open and run
1. Clone the repository and open it in a recent stable Android Studio.
2. Allow Gradle sync to complete.
3. Install Android SDK Platform 36 if prompted.
4. Run the `app` configuration on an Android 16 emulator or compatible device.

Command-line build (Android SDK and Gradle required):
```bash
gradle assembleDebug
```
Run unit tests:
```bash
gradle test
```
Task names can vary by configured variants and Gradle setup; inspect the Gradle task list if a task is unavailable.

## Project map
- `app/src/main/java/com/nestnote/app/MainActivity.kt` — Compose UI and interactions.
- `app/src/main/java/com/nestnote/app/NoteStore.kt` — note model and local persistence.
- `app/src/androidTest/java/com/nestnote/app/NestNoteE2ETest.kt` — Compose/instrumentation workflow tests.
- `app/build.gradle.kts` — Android app configuration.
- `build.gradle.kts`, `settings.gradle.kts` — root Gradle configuration.
- `.github/workflows/` — CI and release automation.

## Working agreements
1. Work in a feature branch and submit a pull request.
2. Keep PRs focused and describe behavior, tests, and known limitations.
3. Preserve existing note data and storage format unless a migration is designed and tested.
4. Keep editor drafts and preference state explicit; test recreation and dismissal paths.
5. Use Material 3 and preserve accessibility labels, touch targets, and readable contrast.
6. Do not claim tests passed based only on compilation or an in-progress CI run.
7. Do not merge PRs without the repository owner's explicit approval.

## CI and UI testing
- Required Android CI is configured for API 36 and should be checked on the exact latest commit.
- Separate emulator UI tests are manual-only by project decision; failures are not a required CI gate.
- Manual-only does not prove UI behavior. Mention remaining manual-validation risk where relevant.
- Physical-device testing is not claimed by this guide.

## Current Phase 2 focus
See [ROADMAP.md](ROADMAP.md). Active branch: `feature/phase-2-note-editor`; [draft PR #2](https://github.com/dukeoftoad/toadNOTES/pull/2).

Current early increment:
- Editor title/body/category/color/reminder use saveable Compose state.
- Explicit close and Cancel ask before discarding meaningful changes.
- Existing NoteStore persistence schema is unchanged.

Before Phase 2 is considered done:
- Restore editor visibility and selected note after activity recreation.
- Ensure system/back and bottom-sheet dismiss requests cannot bypass unsaved-change confirmation.
- Verify create/edit behavior is consistent.
- Run latest normal Android CI and resolve failures.
