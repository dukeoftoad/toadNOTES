# NESTNOTE Release & Validation Checklist

Last updated: 2026-10-24

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
- [ ] Verify lifecycle `ON_STOP` and editor disposal synchronously flush the latest draft before the editor is hidden; test immediate backgrounding/removal during the debounce interval. This reduces lifecycle-loss risk but cannot guarantee writes when the process is killed without callbacks.
- [ ] Stale editor drafts cannot overwrite a newer saved note.
- [ ] Checklist toggling, list continuation, cursor/selection, and trailing newlines behave correctly.
- [ ] Settings and appearance choices persist after restart.
- [ ] Light/dark palettes maintain readable contrast and visible selected states.
- [ ] Test narrow screens, large font/display scaling, TalkBack labels, and touch targets.
- [ ] Verify keyboard focus, scrolling, IME insets, and edge-to-edge system bars on device/emulator.

## Markdown external-open and zoom regression checks
- [ ] Open a `.md` file from Android Files using the app's external-open action; confirm it opens in the viewer and is not silently imported or saved as a note.
- [ ] Confirm explicit multi-file import still imports files as intended and remains distinct from external open.
- [ ] Verify decrease/increase, slider, percentage label, 50% and 200% bounds, and 5% steps.
- [ ] Verify restore-default behavior and that the configured default under Settings → Note editor persists after app restart.
- [ ] Open another external Markdown file and confirm the configured default is applied.
- [ ] Verify long content remains readable/scrollable and check narrow screens, font scaling, TalkBack semantics, and light/dark themes.
- [ ] Record exact-head CI and device/emulator results; do not mark unchecked items complete without evidence.

## Private Vault release gate
- [ ] **Release blocker:** validate the current schema-v2 `DraftStore` envelope implementation on the exact candidate: private drafts must use the dedicated Android Keystore AES-GCM key and explicit classification; missing/invalidated keys, tampered envelopes, and classification mismatches must fail closed; legacy plaintext must never be reclassified as public. Automated tests and exact-head CI must pass, and process-death/device recovery still needs recorded evidence.
- [ ] Background the app while the vault list is open; verify return requires authentication.
- [ ] Background the app while a private note editor is visible; verify no editor content reappears before reauthentication.
- [ ] Recreate the activity/process during vault/editor use; verify private content is not restored into a visible editor while locked.
- [ ] Verify biometric success, cancellation, failure, unavailable hardware, enrollment changes, and disabled-biometric password fallback; only explicit success unlocks.
- [ ] Round-trip archive payloads; verify archive selection excludes Private Vault notes and private notes stay excluded from ordinary views/search/insights. Also verify private drafts are not written/read as plaintext.
- [ ] Verify lockout escalation, cooldown boundary, persistence after restart, 15-minute cap, monotonic behavior under forward/backward wall-clock changes, and bounded behavior after device reboot. Automated regression coverage exists; device verification remains required.
- [ ] Verify FLAG_SECURE on vault and private editor through Recents snapshots, rapid navigation, and lifecycle transitions on supported Android versions.
- [ ] Confirm security copy accurately explains that the vault password derives a wrapping key for the independent Private Vault data key, and that portable archives are password-protected and exclude Private Vault notes.
- [ ] Record exact-head CI result and device/emulator evidence; source inspection alone is not sufficient.

## 3. Backup, encryption, and recovery
- [ ] Create a password-protected encrypted archive and confirm expected public note fields round-trip.
- [ ] Verify encrypted archive/restore is **public-notes-only**: voice audio attachments and Private Vault notes are excluded, and the UI explains the archive scope.
- [ ] Restore a password-protected `.nestnote` archive; reject incorrect passwords and tampered archives.
- [ ] Preview restore additions/updates before applying.
- [ ] Confirm newer incoming notes update existing notes; older/equal timestamps do not overwrite.
- [ ] Confirm notes absent from the import are retained.
- [ ] Reject malformed/tampered archives, unsupported payload schemas, duplicate/non-positive IDs, invalid fields, and oversized payloads without partial mutation.
- [ ] Test storage commit failure and verify the editor does not report a successful save.
- [ ] Test corrupt local data and confirm original bytes remain recoverable.
- [ ] On a device with legacy notes, validate migration commits encrypted content before deleting plaintext.
- [ ] Test missing/invalid Android Keystore key recovery and repeat launches.
- [ ] Verify v2 AES-GCM ciphertext authentication rejects modified bytes without overwriting the original payload.
- [ ] Verify existing v1 encrypted records migrate to v2 and remove v1 only after the v2 write commits.
- [ ] Verify the v2 key alias is retained across app upgrades and document recovery limits if the Android Keystore key is invalidated.
- [ ] Verify the shipped manifest and Android backup/data-extraction behavior on supported versions.
- [ ] Confirm privacy copy states local storage is encrypted and portable archives require a separate password.

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


## Private draft confidentiality release gate

- [ ] Private draft files use AES-GCM with the dedicated Android Keystore draft key; inspect on-disk bytes to verify title/body are absent.
- [ ] Missing/unavailable keys, wrong keys, tampered ciphertext, malformed envelopes, and unsupported schemas fail closed without plaintext fallback.
- [ ] Draft schema persists `privateNote`; missing classification and both public/private envelope mismatches are rejected.
- [ ] Ordinary orphan recovery excludes private drafts while the vault is locked; private-draft recovery is only reachable through the authenticated vault editor flow.
- [ ] Legacy plaintext draft files are never silently migrated as public and are rejected/removed when discovered. Do not claim forensic secure erase.
- [ ] Automated tests cover ciphertext-at-rest, wrong key, tampering, missing classification, classification mismatches, legacy plaintext rejection, and ordinary orphan filtering.
- [ ] Exact-head CI passes and the diff has been reviewed. Do not merge with any of these checks unverified.
