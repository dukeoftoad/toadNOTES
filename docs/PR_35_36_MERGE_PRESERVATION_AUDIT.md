# PR #35/#36 merge-preservation audit and recovery

Tracking issue: [#28](https://github.com/dukeoftoad/toadNOTES/issues/28)

## Scope and merge points

- PR #35 is merged as `8ac92b08ce18767dbeb886d36487ff4e5486f3ba`.
- PR #36 is merged as `394c59fa5134c842ff2e04717f57b19ef147da61`.
- PR #36's tested pre-merge head was `b7a79b83e73f6682bbdac4b28a46bfee5ab8f0f5`; exact-head Android CI run [39052081848](https://github.com/dukeoftoad/toadNOTES/actions/runs/39052081848) passed unit tests, API 36 debug build, minified release build, and instrumentation-test compilation.
- The CI artifact list for that run exposed `unit-test-reports`, not an APK artifact. This does not establish post-merge CI or release publication.

## Confirmed preservation finding

The PR #36 patch removed several tests from `VoiceRecordingLifecycleTest.kt` while simplifying/replacing the test suite. In particular, the patch removed fake-capture tests for ordered PCM chunks, empty capture, simulated read failure/release, overflow behavior, and ensuring overflowed partial output is not committed. It also removed a latch-based pause/resume/stop race test and the explicit repeated-stop assertion. These are test-coverage losses; the patch alone is not evidence that the runtime feature itself was deleted.

The current implementation still provides the `PcmCapture` / `PcmCapturePump` test seam, so the capture tests can be restored against the current implementation without reverting production code. The current lifecycle API has no `FAILED` state or `fail()` transition, so that old assertion should not be copied verbatim; failure behavior should be tested at the capture-pump boundary instead.

PR #36 also removed the standalone JVM acceptance section and historical CI evidence from `RELEASE_CHECKLIST.md`. The general candidate checklist correctly remains unchecked because historical CI on commit `618aa0896bcbc35b6ef912fdd9e72a8a38c87ef2` does not prove the current merge commit was tested.

## Recovery in this PR

- Restore/add JVM regression tests for fake PCM capture ordering, clean empty capture, capture read failure and resource release, size overflow, prevention of partial-clip commit, repeated stop, and pause/resume/stop concurrency.
- Restore a clearly labelled historical acceptance-evidence section to `RELEASE_CHECKLIST.md`, without checking off current-candidate or release items.
- Keep this audit report as a durable record of the finding and validation boundaries.

## Remaining validation gates

- Run Android CI on the exact head of this recovery PR and record the resulting run.
- Run post-merge CI on the resulting `main` commit after merge.
- Verify whether an APK artifact is produced by the intended workflow; do not infer APK publication from a successful build or unit-test report.
- Complete the Phase 8 security/diff review: encrypted/plaintext boundary, attachment deletion and Undo, permissions, manifest compatibility, and partial-clip commit behavior.
- Device/emulator audio validation remains archived by project decision and is not claimed here.

## History safety

This recovery is additive: it does not revert either merged PR, rewrite history, retarget PR #37/#38, or change their branches. Any such operation requires explicit maintainer approval.
