# F-Droid metadata preparation record (not submitted)

This document records the metadata prepared for a possible merge request to the official `fdroiddata` repository. It is not itself an official metadata submission and does not publish NESTNOTE to F-Droid.

## Prepared metadata fields

- **Package ID:** `com.nestnote.app`
- **License:** `GPL-3.0-only`
- **Source code:** https://github.com/dukeoftoad/toadNOTES
- **Issue tracker:** https://github.com/dukeoftoad/toadNOTES/issues
- **Current release candidate:** `v1.4.7`. This candidate is not a published upstream release until the `v1.4.7` tag and GitHub Release are verified. Recheck the [GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases) page before submitting metadata; do not label this candidate as published.
- **Build system:** Gradle / Android Gradle Plugin
- **Build environment observed in CI:** Java 17, Gradle 8.13, Android SDK platform 36, build tools 36.0.0
- **Minimum SDK:** 26
- **Suggested category:** Writing
- **Suggested summary:** Local-first encrypted notes with Markdown and voice attachments.
- **Suggested description:** NESTNOTE is a native Android note-taking app for creating, organizing, searching, archiving, and backing up notes. It supports categories, checklists, Markdown formatting and preview, themes, voice recording with a review-before-attach flow, WAV import, and inline playback cards for multiple audio attachments. Notes are stored locally; note payloads are encrypted using AES-GCM with a key held by Android Keystore. The app has no account-based cloud sync. Portable note transfer uses password-protected encrypted archives; Private Vault notes and audio attachments are excluded.

## Proposed build recipe — validation required before submission

Use a pinned upstream tag that includes the GPL license file and all final metadata-related fixes. Confirm the selected tag contains those changes before submitting.

The project Gradle configuration accepts `-PversionName` and `-PversionCode`. A candidate F-Droid build should use a monotonically increasing numeric version code that is stable for the release. Choose and verify a monotonically increasing numeric version code for the exact upstream tag. Do not assume the GitHub Actions run number is the F-Droid version code; the actual source-build recipe must set it deterministically.

Candidate build commands in the existing CI toolchain (validate these for the exact tag before submission):

```sh
gradle --no-daemon testDebugUnitTest -PversionName=1.4.7 -PversionCode=REPLACE_WITH_VERIFIED_MONOTONIC_CODE
gradle --no-daemon assembleRelease -PversionName=1.3.11 -PversionCode=<monotonic-version-code>
```

This is a starting point, **not a verified F-Droid build recipe**. The final `fdroiddata` build block must use the syntax accepted by current F-Droid tooling and should be tested with `fdroid build` in a compatible build environment. Do not require upstream release signing secrets. F-Droid normally signs its own built APKs.

## Pre-submission gates

- [ ] Merge the license and documentation change.
- [ ] Confirm copyright holders/contributors have authority to distribute all NESTNOTE-authored code under GPL-3.0-only.
- [ ] Audit every direct/transitive dependency and vendored source file, including the Markor parser's retained CC0/Unlicense notice.
- [ ] Check artwork/fonts and other assets for redistribution-compatible licenses.
- [ ] Publish a new version tag containing the license file.
- [ ] Validate source build in the current F-Droid build environment.
- [ ] Verify deterministic versionName/versionCode and the actual output APK's package ID.
- [ ] Confirm the upstream Git tag is immutable and points to the intended source commit.
- [ ] Create the metadata entry in the official `fdroiddata` repository and submit it for review.

## Current limitations

The GitHub Actions check for PR #23 was observed in progress. No completed result for a clean F-Droid build, device installation, reproducibility comparison, or official metadata lint is available yet. Do not mark those tasks as passed until their logs/results are recorded.
