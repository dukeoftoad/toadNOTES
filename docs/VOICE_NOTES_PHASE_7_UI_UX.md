# Voice Notes Phase 7 — UI/UX Quality

Tracking issue: #28  
Parent implementation: #35  
Scope: user-facing voice-note interactions, accessibility, and regression handling.

## 7.1 Interaction states and recording flow
- [x] Audit editor styling against existing NESTNOTE typography, spacing, colors, and components. (Code review: voice UI uses the existing Material 3 Card/Button/OutlinedButton/Text components, theme typography and colorScheme, consistent 14 dp card padding/10 dp vertical spacing, and 8 dp control spacing; status/helper text uses onSurfaceVariant.)
- [x] Make Record, Pause, Resume, Stop, Play, and playback Stop controls clearly labeled, easy to tap, and visually distinct by state. (Text labels accompany icons; action labels change between Play/Stop and Pause/Resume.)
- [x] Show recording/playback state at a glance; prevent invalid simultaneous actions. (Record gating covers startup/permission/recording/playback; playback controls disable during recording and other playback.)
- [x] Show elapsed recording time and playback progress/duration where available; handle unknown duration gracefully. (Elapsed timer, playback progress and tested duration fallback implemented.)
- [x] Distinguish a new unsaved recording from recordings already saved to the note. (New staged rows display “Not yet saved to note”.)
- [x] Prevent rapid repeated taps from creating duplicate recordings, saves, or playback sessions. (Record gate and guarded playback startup.)
- [x] Verify empty, one-recording, multiple-recording, long-title, error, and restored-note states. (Code-path review: empty state has “No recordings yet”; attachments render from the store in an indexed list; row titles are in a weighted Column with wrapping Text; status surfaces permission/start/save/playback errors; noteId effect reloads persisted attachments and refreshes the count. Static/code review only; no device verification claimed.)

## 7.2 Feedback, errors, and destructive actions
- [x] Show concise permission-denied guidance and a clear retry/settings path without repeatedly prompting. (Denial status and explicit Open app settings action; another Record tap retries.)
- [x] Provide feedback for recording started, paused, resumed, saved, stopped, discarded, playback started/stopped, and failures. (Status text covers recording start/pause/resume/finish/save/failures; playback start/manual stop/natural completion/failure; discard closes with a context-specific snackbar.)
- [x] Confirm destructive discard/delete actions where needed; explain what is removed and preserve previously saved attachments when discarding an editor draft. (Discard confirmation explains staged-vs-saved behavior; editor delete has confirmation and note deletion retains an Undo window before audio cleanup.)
- [x] Ensure unsaved-change warnings accurately reflect adding, removing, saving, or discarding a recording. (Adding a recording marks editor dirty; the warning explains staged recordings are discarded while existing saved audio is kept. There is no per-attachment remove action in this UI.)
- [x] Display the 25 MiB limit and explain what happens when the limit or a recording error is reached. (Visible point-of-use disclosure says partial audio is discarded.)
- [x] Keep loading/disabled states understandable and leave controls recoverable after failures. (Starting/permission labels and gates; recording failures clear recording state and abandon audio focus; playback failures reset playback state.)

## 7.3 Backup disclosure and accessibility
- [x] Clearly disclose at the point of use and in backup/settings UI that v1 backups are notes-only and exclude voice audio. (Voice recording card explicitly says audio is excluded from note backups; Backup & privacy settings explain v1 backups contain notes/settings only and excluded audio will not return on restore.)
- [x] Provide meaningful screen-reader labels and state descriptions with logical focus order; never rely on color or icons alone. (Record/Pause/Resume/Stop and per-recording Play/Stop actions expose explicit action descriptions and dynamic availability/playback state descriptions; visible text labels remain alongside icons. Controls are composed in reading order: Record, Pause/Resume, Stop, then attachment rows.)
- [x] Check touch targets, text scaling, light/dark themes, contrast, and narrow-screen layouts using static/code-level checks where feasible. (Material 3 Button/OutlinedButton components provide standard minimum interactive sizing; typography and surfaces use MaterialTheme roles, and attachment labels wrap in a weighted column. Reviewed theme-based text/surface colors and responsive text wrapping statically; no device/visual verification claimed.)
- [x] Keep controls usable with keyboard/navigation accessibility where supported; avoid losing focus after state changes. (Controls remain native Compose Material buttons with standard focus/click semantics and deterministic composition order; state updates do not conditionally remove the active recording-row control except when the attachment list itself changes. Static/code review only.)

## 7.4 Lifecycle and regression checks
- [x] Review backgrounding, audio-focus interruptions/incoming calls, rotation/configuration changes, and process recreation; add automated or code-level checks where feasible. (Static lifecycle review: ON_STOP invalidates pending permission/startup, stops capture/playback and abandons audio focus; disposal removes observer and stops sessions. Rotation/configuration disposal stops active sessions rather than silently continuing; encrypted attachment store/manifest support reload after recreation. Existing VoiceRecordingLifecycleTest covers terminal stop/failure/discard states, invalid transitions, races, and late completion.)
- [x] Review recovery paths for permission denial, capture/playback failures, discard, and preservation of saved attachments. (Permission policy tests cover deny/retry/stale callback; capture tests cover read failure and overflow cleanup; playback callbacks clear session/state and report errors; discard removes staged identities before waiting; editor discard preserves pre-existing saved attachments. Audio-focus interruption now explicitly abandons the listener and prompts retry.)
- [x] Capture and resolve UI regressions found during implementation. (Code review found the audio-focus interruption callback cleared the focus-listener reference without explicitly abandoning focus; corrected the callback to abandon the requested focus and provide a clear retry message. No further code-level regressions identified in this static review.)
- [ ] ~~Manual emulator and physical-device usability pass~~ Archived by project decision; not a release gate.

## Implementation progress (validation pending)
- Record startup now has a single-action gate covering active recording, startup, permission requests, and playback; a JVM regression test covers each blocked state.
- Recording UI now shows an elapsed `mm:ss` indicator and distinguishes paused from active capture.
- Permission denial now provides concise guidance and an explicit app-settings action; permission-policy tests already cover denial, retry, and stale callbacks.
- These changes are not marked complete until the Android CI checks pass on the exact PR head. No emulator/physical-device verification is claimed.

## Follow-up implementation notes (CI intentionally deferred until 7.4 work is complete)
- Added a shared Record action gate covering active recording, startup, pending microphone permission, and active playback, with JVM regression coverage for each blocked state.
- Added an elapsed recording timer and visible active/paused/starting feedback.
- Added permission-denial guidance with an explicit app-settings action and invalidated pending permission/startup UI state when the app leaves the foreground.
- Marked newly staged attachments as not yet saved to the note and added descriptive content descriptions to recording/playback controls.
- Playback start/stop status feedback is visible; saved rows display duration derived from the documented encrypted envelope and PCM16 mono 16 kHz format, with an explicit unknown-duration fallback and JVM tests.
- Added point-of-use disclosure for the 25 MiB recording limit and that a limit/microphone failure discards partial audio. The Backup & privacy settings already explain that v1 backups contain notes/settings only and excluded voice audio will not return on restore.
- Lifecycle review: backgrounding stops capture/playback and abandons audio focus; disposal removes the observer and stops active sessions. Configuration changes therefore stop active capture rather than silently continuing; process recreation relies on the durable encrypted store/manifest and must not be represented as device-tested.
- Static UI review: recording controls use the app Material theme and text labels in addition to icons; saved attachment labels wrap in a column to avoid crowding narrow layouts. Emulator/physical-device testing remains archived.
- Automated CI has intentionally not been run yet, per the work sequence: finish review and implementation through 7.4 first, then run CI on the exact PR head. Keep checklist boxes open until the code and test evidence are reviewed; no device verification is claimed.

## Completion evidence
Mark an item complete only with code review, automated test, or recorded evidence. Do not claim device verification without evidence. Emulator and physical-device execution remains archived and is not required for this milestone.


### 7.1 completion evidence (PR #37 code review)
- Reviewed `VoiceNoteAudio.kt` against the existing Material 3 design system: themed Card and buttons, theme typography/color roles, consistent spacing, and text labels alongside icons. No bespoke colors or typography were introduced.
- Walked the empty, single/multiple attachment, long row-title, permission/startup/playback error, and restored-note code paths. The empty-state text is explicit; attachments are rendered from the durable store; each row uses a weighted text column so titles can wrap; status feedback covers asynchronous failures; and changing `noteId` reloads the stored attachment list and count.
- This is a static/code-level review, not a visual/device test. Per project decision emulator and physical-device execution remain archived. Exact-head CI remains deferred until all Phase 7 tasks through §7.4 are complete.


### 7.2 completion evidence (PR #37 code review)
- Recording status covers active start, pause, resume, finish, successful encrypted save, limit/capture failures, and audio-focus interruption. Playback reports start, explicit stop, natural completion, unavailable focus, and errors.
- Recovery path now clears recording/startup state and abandons audio focus after capture/startup failures so Record can be tried again.
- Editor discard confirmation distinguishes a new draft (including its staged recordings are discarded) from an existing note (new staged audio is discarded but previously saved recordings are preserved). Closing via discard reports the outcome in a snackbar.
- Editor deletion now has a confirmation dialog explaining the Undo window; attachment cleanup still waits until the note deletion remains persisted after Undo expires.
- Unsaved-change tracking already includes newly staged voice attachments. The warning now explains their fate accurately. The current UI has no per-attachment removal action, so no unsupported removal workflow is claimed.
- Static/code review only; exact-head CI is still deferred until all Phase 7 work through §7.4 is complete. No device verification is claimed.


### 7.3 completion evidence (PR #37 code review)
- Backup exclusion is disclosed both beside the voice recording controls and in Backup & privacy settings; v1 restore does not claim voice-audio recovery.
- Added explicit Compose accessibility content descriptions and dynamic state descriptions for Record, Pause/Resume, Stop recording, and each attachment's Play/Stop playback action. Visual text labels remain present, so actions are not conveyed by icon or color alone.
- Static review confirmed themed Material 3 controls, theme typography/color roles, standard Material button interaction sizing, a deterministic reading order, and weighted/wrapping attachment text for narrow layouts and larger text. Keyboard/navigation support is provided through standard Compose Material button semantics.
- Validation is static/code-level only; no emulator or physical-device accessibility, contrast, scaling, or visual test is claimed. Exact-head CI remains deferred until all Phase 7 work through §7.4 is complete.


### 7.4 completion evidence (PR #37 code review)
- Lifecycle: backgrounding stops active recording/playback, clears pending permission/startup state, and abandons audio focus. Disposable cleanup unregisters the observer and stops sessions. Configuration changes dispose the active UI session instead of allowing capture to continue invisibly. Persisted encrypted attachment data and its manifest are reloaded when the note editor is recreated.
- Existing JVM regression coverage in `VoiceRecordingLifecycleTest` exercises pause/resume/stop ordering, terminal failure/discard, repeated operations, overflow/read-failure cleanup, discard-vs-completion races, and pause/resume-vs-stop races. `VoicePermissionPolicyTest` covers denied permission, retry, stale results, action gating, and duration edge cases.
- Reviewed recovery for capture/startup errors, playback errors/natural completion, permission denial, draft discard, and saved-attachment preservation. Fixed the audio-focus interruption callback to abandon the requested focus and present an explicit retry instruction.
- Static/code review only. No emulator/physical-device behavior is claimed; those checks remain archived by project decision. CI remains pending: the Android CI workflow is configured for pull requests targeting main, pushes to main, or manual workflow_dispatch. PR #37 currently targets docs/voice-backup-exclusion, so no exact-head run was created. The connected GitHub actions do not expose workflow_dispatch, and the PR base was not changed without approval.


### Current validation status (2026-10-17, after retargeting)
- PR #37 has been retargeted to `main` with maintainer approval.
- Exact-head Android CI is still required. Existing test coverage is documented above, but no passing CI result is claimed until the workflow completes on the current head.
- The 7.4 review and JVM test coverage are recorded; emulator/physical-device E2E remains archived by project decision and has not been run. This means 7.4's code-review/regression-review tasks are addressed, but full runtime/device validation is not claimed.
