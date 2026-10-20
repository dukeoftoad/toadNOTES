# NESTNOTE Release & Validation Checklist

Last updated: 2026-10-17

Use this checklist for every candidate. A successful build is necessary but does not establish release readiness. Record the exact commit, CI run, device, results, and accepted risks in WORKLOG.md. Do not mark a check complete without evidence.

## 1. Candidate and CI
- [ ] Candidate commit SHA recorded and matches the commit being tested.
- [ ] Required Android CI passes on that exact commit.
- [ ] Unit tests pass.
- [ ] Debug APK builds successfully for API 36.
- [ ] Minified/R8 release APK builds successfully.
- [ ] Instrumentation tests compile; actual execution on a device/emulator is separately recorded.
- [ ] No unexplained build warnings, vulnerable dependencies, or new manifest permissions remain.
- [ ] Release notes, version, and known limitations reviewed.

## 2. Core notes and editor
- [ ] Create, edit, save, cancel, pin, archive, restore, search, sort, and delete/undo work.
- [ ] Unsaved changes remain protected through Back, Cancel, rotation, activity recreation, and sheet dismissal.
- [ ] Stale editor drafts cannot overwrite a newer saved note.
- [ ] Checklist toggling, list continuation, cursor/selection, and trailing newlines behave correctly.
- [ ] Settings and appearance choices persist after restart.
- [ ] Light/dark palettes maintain readable contrast and visible selected states.
- [ ] Test narrow screens, large font/display scaling, TalkBack labels, and touch targets.
- [ ] Verify keyboard focus, scrolling, IME insets, and edge-to-edge system bars on device/emulator.

## 3. Backup, encryption, and recovery
- [ ] Export a backup and confirm expected note fields round-trip.
- [ ] Verify backup/restore is **notes-only**: voice audio attachments are excluded, and the export/restore UI clearly warns that recordings are not included.
- [ ] Restore versioned backup and legacy JSON-array backup.
- [ ] Preview restore additions/updates before applying.
- [ ] Confirm newer incoming notes update existing notes; older/equal timestamps do not overwrite.
- [ ] Confirm notes absent from the import are retained.
- [ ] Reject malformed JSON, unsupported schema, duplicate/non-positive IDs, invalid fields, and oversized payloads without partial mutation.
- [ ] Test storage commit failure and verify the editor does not report a successful save.
- [ ] Test corrupt local data and confirm original bytes remain recoverable.
- [ ] On a device with legacy notes, validate migration commits encrypted content before deleting plaintext.
- [ ] Test missing/invalid Android Keystore key recovery and repeat launches.
- [ ] Verify v2 AES-GCM ciphertext authentication rejects modified bytes without overwriting the original payload.
- [ ] Verify existing v1 encrypted records migrate to v2 and remove v1 only after the v2 write commits.
- [ ] Verify the v2 key alias is retained across app upgrades and document recovery limits if the Android Keystore key is invalidated.
- [ ] Verify the shipped manifest and Android backup/data-extraction behavior on supported versions.
- [ ] Confirm privacy copy states local storage is encrypted and exported JSON is plaintext.

## Historical voice-recording JVM acceptance evidence

This section records historical evidence only; it does **not** mark the current candidate, current `main`, or a release as validated.

- [x] Recording state transitions are isolated in an Android-independent lifecycle component.
- [x] Fake PCM capture regression tests cover ordered chunks, empty capture, read failure/resource release, size overflow, and refusal to publish a partial clip. These tests were restored additively in PR #39; require the exact-head CI result below before treating this recovery as validated.
- [x] Latch-based lifecycle tests cover stop/discard and pause/resume/stop races.
- [x] Historical Android CI run [#39048049532](https://github.com/dukeoftoad/toadNOTES/actions/runs/39048049532) passed on commit `618aa0896bcbc35b6ef912fdd9e72a8a38c87ef2` (2026-10-17): unit tests, API 36 debug build, minified release compilation, and instrumentation-test compilation. This run predates PR #36's merge commit and is not proof that the current candidate passed.
- [ ] Exact-head Android CI for the PR #39 recovery commit passes. Record the run URL before checking this item.
- [ ] Post-merge CI and APK artifact availability are verified for the resulting `main` commit.

Device/emulator execution is not claimed; it remains archived by project decision. General candidate and production-signing checklist items below remain unchecked until independently evidenced.

## 4. Production signing and APK integrity
- [ ] Confirm repository Actions secrets exist without exposing values: KEYSTORE_BASE64, KEY_ALIAS, KEYSTORE_PASSWORD, KEY_PASSWORD.
- [ ] Confirm KEYSTORE_BASE64 decodes to the approved permanent production keystore; no keystore bytes are committed or logged.
- [ ] Confirm the release workflow runs for version tags matching `v*.*.*` or manual dispatch with a validated `release_tag`; it does not trigger on ordinary pushes to `main`.
- [ ] Confirm missing or invalid signing secrets fail the tagged workflow and prevent publication.
- [ ] Confirm release APK is minified/resource-shrunk and named NESTNOTE-vX.Y.Z.apk.
- [ ] Verify the published APK signature using Android apksigner.
- [ ] Independently compare the signing certificate fingerprint with the recorded production fingerprint.
- [ ] Install and smoke-test the exact APK downloaded from GitHub Releases.
- [ ] Test upgrade installation over the previous production version where available.
- [ ] Confirm release notes, privacy disclosures, artifact contents, and known limitations.

Release page: https://github.com/dukeoftoad/toadNOTES/releases  
Release workflow: https://github.com/dukeoftoad/toadNOTES/actions/workflows/android-release.yml

## 5. Release decision record
- Candidate version/tag:
- Candidate commit SHA:
- Android CI run URL and result:
- Device/emulator model and Android version:
- Automated checks completed:
- Manual checks completed:
- Checks not run:
- Migration/recovery result:
- Production certificate fingerprint independently verified:
- Published APK URL and checksum:
- Known issues / accepted risks:
- Owner approval and date:

Do not publish or merge automatically because a checklist is filled out. Unrun manual checks must be marked Not run. An unsigned inspection artifact or historical CI-signed APK is not a production release.
