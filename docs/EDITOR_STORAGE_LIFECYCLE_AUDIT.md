# Editor, Note Identity, Storage, Backup, and Release Audit

Audit date: 2026-10-17
Baseline: `main` at the time of audit
Scope: pre-implementation review for voice notes. This is a source audit; behavior that needs a running device/emulator is explicitly marked unverified.

## Executive decision

The encrypted note store and voice-attachment storage foundation provide useful safeguards, but voice recording should not be integrated until attachment ownership and editor draft semantics are explicit. The highest-risk boundary is that note metadata and audio are stored separately and therefore cannot be committed in one atomic transaction.

No production/device audio validation is claimed by this report.

## 1. Editor lifecycle

Source: `app/src/main/java/com/nestnote/app/MainActivity.kt`.

Observed:
- Editor selection is represented by saveable `editorNoteId`; `editorNote` is derived from the currently persisted `notes` list.
- Visibility and unsaved-change state are separate saveable booleans.
- Existing-note saves compare the opened draft's `updated` timestamp with the currently saved note to reject a stale draft.
- Explicit close/Cancel paths use discard confirmation when meaningful unsaved changes exist.
- The shared `persist` helper blocks writes during storage recovery, unless the caller explicitly uses the recovery path, and reports a failed save rather than updating in-memory notes.
- Delete is persisted before the editor is dismissed or Undo is offered; attachments are retained during the Snackbar Undo window and cleaned after confirmed deletion.

Risks / requirements:
1. A boolean dirty flag is not a durable representation of a draft. Activity/process recreation must not imply the draft itself was persisted merely because editor identity/visibility state is saveable.
2. A new note has no persisted identity until the app chooses an ID. Recording must not create a durable attachment against a transient/null editor identity.
3. Recording while editing an existing note must not silently save or overwrite text changes, nor bypass the stale-draft guard.
4. A failed note save must not be presented as success, and must not trigger cleanup of the only audio copy.
5. Test every exit path: toolbar close, Back, Cancel, sheet/dialog dismissal, activity recreation, process death, save failure and deletion/Undo.

Required design: define explicit editor states (new/existing, clean/dirty, saving, save failed, discard pending) and a documented attachment commit policy. Avoid a broad editor rewrite unless tests show it is necessary.

## 2. Note identity

Source: `NoteStore.kt`.

Observed:
- `NestNote.id` defaults to `System.currentTimeMillis()`; sample notes use fixed positive IDs.
- Shared collection validation rejects non-positive IDs, duplicate IDs, excessive counts and oversized fields.
- Backup merge matches notes by ID and uses `updated` to select the newer record.

Risks / requirements:
1. Millisecond timestamps are not a uniqueness guarantee; two creations can collide.
2. Import/restore may contain IDs that overlap local notes. The current merge deliberately treats matching IDs as the same logical note, which can update local content rather than create a separate note.
3. Attachment directories are keyed by note ID, so an ID collision can associate audio with the wrong logical note.
4. Existing IDs are persisted data and must not be renumbered during migration.

Required design: use a collision-safe ID allocator compatible with the current Long-based schema, and define how imported ID collisions are resolved before shipping attachment-aware restore. Test concurrent/rapid creation, restored records and attachment ownership.

## 3. Local storage and migration

Source: `NoteStore.kt`.

Observed:
- Local note JSON is encrypted using Android Keystore AES-GCM.
- v2 ciphertext uses a dedicated key alias, random IV and authenticated additional data.
- The loader prefers v2, then migrates authenticated v1 ciphertext, then migrates legacy plaintext.
- Migration saves the new encrypted representation before removing legacy keys in the successful SharedPreferences commit.
- Unreadable/corrupt data returns a recovery-required result rather than replacing it with sample notes.
- The UI blocks ordinary writes while recovery is required.
- Voice attachment data is stored separately in app-private files; orphan cleanup is a helper and must not run when note storage recovery is required.

Risks / requirements:
1. Note metadata and attachment files do not share a transaction. A crash can leave an incomplete attachment or an attachment with no corresponding note.
2. Key invalidation/missing-key behavior must preserve existing encrypted bytes and make recovery limitations clear.
3. Any migration/reconciliation must be idempotent and conservative; it must not delete attachments based on an empty note list produced by a recovery failure.

Required design: version an attachment manifest or otherwise define durable attachment metadata, reconcile only after a successful trusted note load, and make cleanup safe to retry. Never delete orphan candidates while recovery is required.

## 4. Backup and restore

Sources: `NoteStore.kt`, `MainActivity.kt`, `RELEASE_CHECKLIST.md`.

Observed:
- Export uses a versioned JSON envelope (`schemaVersion = 1`) containing note records.
- Import accepts that envelope and a legacy JSON-array format, with size/count/field/ID validation.
- Restore previews a merge before applying it. Existing notes absent from the import remain; incoming notes update a matching ID only when their `updated` value is newer.
- The current backup schema serializes note fields only. It does not include the separate encrypted voice attachment files.
- Exported JSON is plaintext even though local note storage is encrypted.

Risks / decisions:
1. A notes-only restore can restore note text while silently lacking voice data unless the UI and backup format communicate this clearly.
2. A portable voice backup cannot simply copy ciphertext bound to local attachment AAD/key and assume it will decrypt on another installation.
3. A future backup must define portable encryption, key/passphrase handling, limits, partial import behavior, and collision policy.
4. Legacy schema compatibility must remain tested; do not change schema version 1 semantics in place.

**Decision (v1): choose A — backups remain note-only.** Voice-note audio attachments are intentionally excluded from export and restore. The UI and user-facing documentation must disclose that restoring a backup restores notes, not their voice recordings. Do not copy installation-bound attachment ciphertext into a backup. Any future audio backup requires a separately approved, versioned portable archive design and round-trip tests; it is out of scope for v1.

## 5. Manifest and permissions

Source: `app/src/main/AndroidManifest.xml`.

Observed:
- `android:allowBackup="false"` is explicitly set.
- No microphone permission is declared in the inspected manifest.

Requirements before recorder implementation:
- Add microphone permission only with a user-visible recording flow and a clear denial/retry path.
- Request permission at runtime; do not request it on app launch.
- Explain when recording is active and stop/release recorder resources on all exit/error paths.
- Verify backup/data-extraction behavior on supported Android versions and ensure audio is not unexpectedly included in platform backups.

## 6. CI and release workflow

Sources: `.github/workflows/android-ci.yml`, `.github/workflows/android-ui-tests.yml`, `.github/workflows/android-release.yml`, `RELEASE_CHECKLIST.md`.

Observed:
- Android CI runs unit tests, debug build, minified release build and instrumentation-test compilation.
- Emulator UI tests are a separate manually dispatched workflow. Compilation in CI is not evidence that emulator tests ran.
- Production release workflow triggers on version tags matching `v*.*.*` and manual dispatch with a validated `release_tag`.
- It runs unit tests, builds a versioned release APK, requires signing secrets, signs/verifies the APK, and publishes a GitHub Release.
- The release workflow does **not** currently trigger on pushes to `main`.
- Workflow secret mapping is `KEYSTORE_BASE64`, `KEY_ALIAS`, `KEYSTORE_PASSWORD`, and `KEY_PASSWORD`. README and release checklist must use those exact names.

Required follow-up:
1. Correct README/checklist to match the actual tag/manual-dispatch trigger and secret names (unless a deliberate workflow change is separately approved).
2. For each voice-note PR, require exact-head Android CI and explicitly record whether the emulator workflow was run.
3. Add tests for recorder lifecycle, permission denial, cancellation, interrupted writes, corrupted attachment data, restore policy and delete/Undo.
4. Before release, install and test the exact signed APK, verify the production certificate fingerprint, and perform upgrade/migration checks.
5. Never describe CI success or instrumentation compilation as physical-device audio validation.

## 7. Implementation gates

- [ ] Decide and test when a new note ID becomes permanent.
- [ ] Add collision-safe Long ID allocation without changing existing IDs.
- [ ] Define draft/save/discard behavior for recording and attachment-only changes.
- [ ] Ensure an attachment is not committed as valid until its encrypted payload is complete and authenticated.
- [ ] Add a durable attachment manifest or a documented reconciliation strategy.
- [ ] Keep orphan cleanup disabled during note-store recovery.
- [x] Decide and communicate v1 backup scope: notes-only; voice audio is excluded. Any future audio backup requires a separately versioned portable archive.
- [ ] Preserve legacy note backup and storage migration compatibility.
- [ ] Correct release checklist trigger/secret discrepancies.
- [ ] Pass exact-head CI and run emulator/device audio tests before production claims.

## Out of scope / unverified

This report is based on source inspection. It does not prove behavior under actual process death, device audio hardware, Android Keystore invalidation, permission dialogs, or production upgrade installation. Those require automated fault-injection tests where feasible and explicit emulator/device validation.

## PR #37 follow-up: Phase 7 integration status (2026-10-17)

This addendum records implementation changes made after the baseline audit above; it does not rewrite the historical observations.

- The Phase 7 branch now uses the current `main` `NoteStore.kt` implementation, preserving restore collision remapping and draft-ID collision checks.
- Restore preview remaps an incoming draft ID when it collides with an existing local note or the active editor draft. Saving refuses a draft ID that has become occupied rather than silently moving staged audio to another identity.
- `VoiceRecordingLifecycle.State.FAILED` and the synchronized `fail()` transition were added. A failed capture cannot be restarted, paused, resumed, or stopped into a success state. The lifecycle regression suite includes a terminal-failure test while retaining the recovered fake-PCM and concurrency coverage.
- `RELEASE_CHECKLIST.md` now distinguishes historical CI evidence from current-head validation and leaves current candidate gates unchecked.

Validation boundary: these source changes have been pushed to PR #37, but the GitHub compare still reports the branch as diverged from `main`. Exact-head CI has not been verified, and no local build or device test is claimed. Do not merge until the complete diff is reconciled and current-head CI passes.
