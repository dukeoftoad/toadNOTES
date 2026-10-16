# F-Droid metadata proposal (not yet submitted)

This is a working draft for an eventual merge request to the official `fdroiddata` repository. It is not a valid repository URL and does not itself publish NESTNOTE to F-Droid.

## Candidate metadata

- **Package ID:** `com.nestnote.app`
- **License:** `GPL-3.0-only`
- **Source code:** https://github.com/dukeoftoad/toadNOTES
- **Issue tracker:** https://github.com/dukeoftoad/toadNOTES/issues
- **Current upstream release audited:** `v1.2.4` (published 2026-10-15)
- **Build system:** Gradle / Android Gradle Plugin
- **Build environment observed in CI:** Java 17, Gradle 8.13, Android SDK platform 36, build tools 36.0.0
- **Minimum SDK:** 26
- **Suggested category:** Writing
- **Suggested summary:** Local-first notes with encrypted storage and Markdown preview.
- **Suggested description:** NESTNOTE is a native Android note-taking app for creating, organizing, searching, archiving, and backing up notes. Notes are stored locally; note payloads are encrypted using AES-GCM with a key held by Android Keystore. The app supports categories, checklists, Markdown preview, themes, and user-controlled JSON backup and restore. It does not provide account-based cloud sync.

## Candidate build recipe — validate before submission

Use a **new upstream tag that includes the GPL license file and all final metadata-related fixes**, not the existing `v1.2.4` tag if it predates those changes.

The project Gradle configuration accepts `-PversionName` and `-PversionCode`. A candidate F-Droid build should use a monotonically increasing numeric version code that is stable for the release. For example, version code `10205` could represent version `1.2.5`, if that release is actually tagged and approved. Do not publish this example as fact.

Candidate build commands in the existing CI toolchain:

```sh
gradle --no-daemon testDebugUnitTest -PversionName=1.2.5 -PversionCode=10205
gradle --no-daemon assembleRelease -PversionName=1.2.5 -PversionCode=10205
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
