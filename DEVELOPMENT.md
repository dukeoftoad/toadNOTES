# NESTNOTE Development Guide

Last updated: 2026-10-12

## Stack and prerequisites
- Kotlin, native Android, Jetpack Compose, Material 3.
- Android target SDK 36; minimum SDK 26.
- Java 17 and Gradle 8.13 (the CI workflow uses these versions).
- Android SDK Platform 36 and Build Tools 36.0.0.

## Set up and build
1. Clone the repository and open it in a recent stable Android Studio.
2. Allow Gradle sync to complete and install SDK components if prompted.
3. Run the app configuration on an Android 16 emulator or compatible device.

Command-line checks:

```bash
gradle --no-daemon testDebugUnitTest
gradle --no-daemon assembleDebug
gradle --no-daemon assembleRelease
gradle --no-daemon assembleDebugAndroidTest
```

CI is defined in .github/workflows/android-ci.yml. It runs unit tests, builds debug and minified release variants, and compiles instrumentation tests. Hosted-emulator execution is intentionally excluded from required CI to avoid subscription/usage costs. To execute instrumentation tests when you have an available local emulator or device, run `gradle --no-daemon connectedDebugAndroidTest`. Manual visual, TalkBack, large-font, or physical-device verification remains separate.

## Code map
- app/src/main/java/com/nestnote/app/MainActivity.kt — dashboard, editor, settings, and Compose interactions.
- app/src/main/java/com/nestnote/app/NoteStore.kt — encrypted local persistence, migration, validation, recovery, and backup/restore.
- app/src/main/java/com/nestnote/app/NoteText.kt — checklist/list operations, selection mapping, and writing counters.
- app/src/test/ — unit and local logic tests.
- app/src/androidTest/ — Compose instrumentation coverage.
- .github/workflows/android-ci.yml — required build/test checks.
- .github/workflows/android-ui-tests.yml — manually triggered UI workflow.
- .github/workflows/android-release.yml — tagged production release pipeline.

## Data safety requirements
1. Preserve user data; do not replace unreadable stored content with starter notes.
2. Keep encryption/migration ordering fail-safe: commit encrypted content before removing legacy plaintext.
3. Validate the complete backup before applying restore; preserve existing notes absent from an import.
4. Keep legacy backup import compatibility unless an explicit migration plan and tests replace it.
5. Do not log or commit signing secrets, keystores, passwords, or note contents.
6. User-exported backup JSON is plaintext. Keep privacy copy consistent with actual behavior.
7. Changes to storage, backup, encryption, manifest backup settings, or release signing require regression tests and updated docs.

## Working agreements
- Use a focused feature branch and pull request.
- Explain user-visible behavior, tests run, CI links, limitations, and migrations in each PR.
- Update README, ROADMAP, RELEASE_CHECKLIST, and WORKLOG when scope/status or validation changes.
- Keep historical work-log entries as history; add a current status note rather than silently rewriting prior events.
- Never report a test, device check, signature verification, or release as successful unless evidence exists for the exact commit/artifact.
- Do not merge or publish without repository-owner approval.
- Prefer accessible Material 3 patterns, clear contrast, semantic labels, scalable text, and usable touch targets.

## Current product and validation state
Phases 1–6 and security/release hardening PRs #1–#7 are merged to main as of 2026-10-12. See ROADMAP.md for implementation priorities and WORKLOG.md for historical evidence.

Still required before claiming production release readiness: real-device/emulator smoke testing, legacy-data migration and Keystore recovery validation, TalkBack and large-font/narrow-screen checks, keyboard/focus/insets checks, release signing certificate fingerprint verification, and installation/upgrade testing of the exact published APK. These checks are not claimed as complete by this guide.


## Mandatory UI/UX design contract
All UI changes must follow [UI_UX_RULES.md](UI_UX_RULES.md). The immediate P0 is note creation/editing and replacing the bottom appearance picker with an animated right-side Appearance panel. Keep the writing canvas dominant; overlay the panel on phones, provide clear colour/background previews and selection state, and preserve the existing save/discard protections. New and existing notes must reuse the same editor and appearance components. Do not expand backend/new features ahead of this P0 unless explicitly approved. Follow the audit → prototype/reference → acceptance criteria → reuse-first implementation → test → accessibility/responsive review → worklog sequence.
