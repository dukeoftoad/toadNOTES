# NESTNOTE Release & Validation Checklist

Use this checklist for each candidate release. A successful build is necessary, but it is not by itself evidence that the app is ready to ship. Record the commit, Android version/device, test result, and any accepted risks in [WORKLOG.md](WORKLOG.md).

## Phase 6 implementation order

Track implementation separately from release sign-off. The intended order is **P0 → P1 → P2**; check an item only after the change and its relevant tests have passed.

### P0 — Editing, saving, and recovery
- [ ] Show clear Saving / Saved / Save failed status for edits and preserve recoverable drafts on failure.
- [ ] Protect editor drafts across Back, rotation, activity recreation, and interrupted editing.
- [x] Reject stale editor drafts when the saved note version has changed (version-guard unit test and CI pass); rapid/repeated-save and full lifecycle testing remain open.
- [x] Reject malformed, oversized, duplicate-ID, and invalid backup records before any mutation (unit-tested; CI passed on 2026-10-12).
- [x] Use a versioned backup schema with legacy migration/import support (schema-version and legacy-array tests pass CI).
- [x] Preview restore additions/updates and apply the validated restore only after full validation (merge and restore-preview implementation; unit tests pass CI).
- [x] Never silently treat unreadable local storage as a fresh installation; preserve original bytes and provide recovery (regression test passes CI).
- [x] Verify regression tests and Android CI on the exact implementation commit `48fd9d36b3ca4a559615bf74d68a149eaeede91c` (run [#38228131828](https://github.com/dukeoftoad/toadNOTES/actions/runs/38228131828), passed 2026-10-12).

### P1 — Writing experience, accessibility, and organization
- [x] Make checklist items interactive in the note body and preserve cursor/selection during list formatting (JVM tests and CI pass; device interaction remains manual).
- [ ] Validate keyboard insets, focus, scrolling, and editor lifecycle on supported devices.
- [ ] Audit foreground/background contrast for all note palettes in light and dark themes.
- [ ] Test appearance-picker selected states, font scaling, display scaling, narrow screens, TalkBack labels, and touch targets.
- [ ] Verify search combines predictably with category/archive filters and sorting; add bulk actions only after existing flows are stable.
- [x] Reconcile current Phase 6 status in ROADMAP.md and WORKLOG.md; historical run notes are retained.

### P2 — Incremental architecture
- [ ] Extract persistence behind a repository interface with focused tests.
- [ ] Move dashboard/editor state into lifecycle-aware ViewModels without a full rewrite.
- [ ] Evaluate Room only when query/data requirements justify migration; document migration and rollback before changing storage.
- [ ] Keep storage migration backward-compatible and prove it with backup/upgrade tests.

## 1. Automated checks

- [x] Android CI passes on the exact candidate commit `48fd9d36b3ca4a559615bf74d68a149eaeede91c` (run #38228131828).
- [x] Unit tests pass (run #38228131828).
- [x] Debug APK builds successfully (API 36, run #38228131828).
- [ ] Release APK builds with the configured R8/resource shrinking settings.
- [x] Instrumentation tests compile (run #38228131828); execution on a device/emulator remains unverified.
- [ ] No new unexplained warnings, dependency vulnerabilities, or manifest permissions are introduced.

## 2. Core note-taking regression

On a clean install and an upgrade install where practical:

- [ ] Create, edit, save, cancel, and delete a note.
- [ ] Confirm unsaved edits are protected through Back/cancel and activity recreation/rotation.
- [ ] Verify pin, archive, restore, sort, search, and category filtering.
- [ ] Verify note colors and editor backgrounds in light and dark themes.
- [ ] Confirm preferences persist after app restart.
- [ ] Test keyboard visibility, scrolling, system bars/edge-to-edge, narrow screens, and larger font/display scaling.
- [ ] Check icon labels, touch targets, contrast, and TalkBack usability.

## 3. Backup and recovery

- [ ] Export notes to a user-selected JSON file and confirm expected fields survive round-trip.
- [ ] Restore a valid backup and confirm new IDs are added, newer copies win, and existing notes are not deleted.
- [ ] Reject malformed JSON, missing required fields, duplicate IDs, invalid values, and oversized inputs without changing existing notes.
- [ ] Confirm restore validates the full import before applying any changes.
- [ ] Verify legacy-record compatibility, including records without a creation timestamp.
- [ ] Test storage/read/write failure paths and confirm saved data is not silently replaced by starter notes.
- [ ] Confirm privacy copy accurately explains whether local storage, automatic backup, and exported files are encrypted or included in backup.

## 4. Android storage and privacy

- [ ] Verify the shipped manifest and applicable backup/data-extraction rules on supported Android versions.
- [ ] Confirm the chosen automatic-backup policy matches actual platform behavior; do not infer all transfer behavior from `allowBackup` alone.
- [ ] If encrypted storage is shipped, test migration from existing plaintext records, repeat launches, corrupt records, missing keys, and recovery guidance.
- [ ] Confirm exported JSON is handled as plaintext unless export encryption is explicitly implemented.
- [ ] Review permissions, exported components, and newly added dependencies.

## 5. APK build and GitHub Release delivery

- [ ] Confirm Android CI runs unit tests, builds the debug APK, compiles instrumentation tests, and builds the minified/R8 release variant on the exact candidate commit.
- [ ] Confirm production publishing is tag-only (`v*.*.*`) and a push to `main` does not create or overwrite a public release.
- [ ] Confirm all four permanent signing Actions secrets are configured without exposing their values: `SIGNING_KEY`, `KEY_ALIAS`, `KEY_STORE_PASSWORD`, `KEY_PASSWORD`.
- [ ] Confirm a tagged build fails closed when signing secrets are missing/invalid and does not publish an unsigned APK.
- [ ] Confirm the published GitHub Release contains `NESTNOTE-vX.Y.Z.apk`, generated release notes, and a verified production signature.
- [ ] Verify the APK certificate fingerprint independently and install/test the published APK before distribution.
- Download page: https://github.com/dukeoftoad/toadNOTES/releases

## 5. Production signing and artifact integrity

- [ ] Confirm the production signing key is available only through the approved secret/managed signing path.
- [ ] Confirm tagged production releases fail if permanent signing configuration is absent or invalid.
- [ ] Verify the generated APK signature and compare its certificate fingerprint with the independently recorded expected production certificate.
- [ ] Ensure a temporary CI key is never used for a public production artifact.
- [ ] Ensure signing secrets, keystore bytes, and private key material are not logged or committed.
- [ ] Confirm the signing-key backup and recovery procedure is documented and access is restricted.
- [ ] Install the signed artifact and test an upgrade over the previous production build where available.
- [ ] Review release notes, version/tag, privacy disclosures, and artifact contents before publishing.

## 6. Release decision

- [ ] All required checks above have an explicit result; unrun manual checks are marked **Not run**, not passed.
- [ ] Remaining risks have a written explanation and explicit owner approval.
- [ ] The release commit and CI run are recorded in WORKLOG.md.
- [ ] Repository owner approves publication. Do not merge or publish automatically as a consequence of checklist completion.

## Result record

Copy this section into WORKLOG.md for each candidate:

- Candidate commit:
- CI run:
- Device/emulator and Android version:
- Manual checks completed:
- Checks not run:
- Signing certificate fingerprint verified:
- Known issues / accepted risks:
- Release decision and owner approval:
