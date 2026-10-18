# Voice Notes Phase 8 — Automated Validation and Release Gates

Tracking issue: #28  
Parent implementation: #35  
Related implementation: #36 and #37 (both merged to `main`)  
Scope: exact-head CI, source security/regression review, APK artifact evidence, and closeout.

## 8.1 Exact-head CI

- [x] Existing emulator-independent regression tests cover manifest validation, encrypted attachment metadata/store behavior, permission policy, PCM format/capture failures and size limits, lifecycle terminal states, and concurrency races.
- [x] Android CI passed on prior candidate commit `4ecf9c075ab706db37e0784121362ba196521d52` in [run 39053007387](https://github.com/dukeoftoad/toadNOTES/actions/runs/39053007387): unit tests, API 36 debug APK, minified release APK compilation, and instrumentation-test compilation.
- [ ] Re-run required CI after the Phase 8 changes on the exact current PR head.
- [ ] Confirm the green run's head SHA exactly matches the final PR head and retain its test report and APK artifacts.

## 8.2 Security and regression review

- [x] Review audio-at-rest handling: attachment payloads stream through AES-GCM encryption into app-private files; authenticated reads do not return plaintext until tag validation succeeds; temporary write files are deleted on failed saves.
- [x] Review attachment ownership and lifecycle: manifest entries are checked against stored payloads; draft discard removes staged payloads and metadata; delete cleanup waits until the Undo window expires; saved attachments are preserved when an existing note's unsaved edits are discarded.
- [x] Review partial-clip failure paths: capture failure and size overflow transition to terminal failure; incomplete recordings are not committed as successful attachments; JVM tests cover fake PCM errors, size limits, and stop/discard races.
- [x] Review permission and manifest compatibility: microphone permission is declared only for the recording feature, and runtime permission is requested in response to Record rather than at app launch. Automatic Android backup remains disabled.
- [x] Confirm v1 backup policy is notes-only; update export success feedback and restore confirmation to explicitly state that voice recordings are excluded.
- [x] Investigate warnings from the successful CI run. Known warnings are deprecated Compose auto-mirrored icons, legacy AudioManager audio-focus APIs, Gradle features deprecated ahead of Gradle 9, and Node.js 20 deprecation notices from GitHub Actions. These are identified, not build failures; broad API/action upgrades are follow-up maintenance and must be revalidated separately.
- [ ] Complete an automated dependency vulnerability/SCA scan; this is not part of the current Android CI workflow and no scan result is claimed.

## 8.3 Merge and artifact verification

- [ ] Merge only after the current exact-head CI is green and review is complete.
- [ ] Record merge commit SHA and verify CI on merged `main`.
- [ ] Confirm the CI run uploads both debug and minified release APK inspection artifacts and links them to the exact workflow run/commit.
- [ ] Production release checks (permanent signing key, certificate fingerprint, published APK integrity, release notes, and installed-APK smoke test) are **not applicable to a production release until an approved version/tag is selected**. Do not treat an unsigned CI APK as a production release.

## 8.4 Evidence and closeout

- [ ] Record final tested and merge SHAs, exact CI run URL/result, and APK artifact URL.
- [ ] Record checks not run and known limitations; do not imply device verification or a published release without evidence.
- [x] Emulator and physical-device execution remains archived by project decision and is not an automated release gate. No device execution is claimed here.

## Source-review findings

- Audio storage uses app-private files, AES-GCM with per-payload random IVs, and AAD bound to the note and attachment identities.
- Attachment ciphertext is excluded from note JSON backups because it is installation-key/identity-bound. Settings and restore confirmation disclose that voice audio does not return with a notes-only restore.
- The manifest is size/count bounded, versioned, and atomically replaced; malformed metadata raises a recovery error rather than deleting payloads.
- `RECORD_AUDIO` is the only explicit app permission added for this feature. No new network permission is declared.
- The current CI uploads test reports and, after this change, debug/release APK inspection artifacts. Those APKs are CI inspection outputs and are not production-signed-release evidence.

## Completion rule

Close Phase 8's automated gate only after the final PR head passes CI, APK artifacts are available, the merged commit is verified, and evidence is recorded. Production signing/publication and device-only checks must remain explicitly pending unless the relevant release is approved and independently verified.
