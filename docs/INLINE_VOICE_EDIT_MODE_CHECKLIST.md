# Inline voice playback in edit mode — implementation checklist

## Acceptance criteria
- [ ] A saved note with a voice attachment shows a Play control in the editor without tapping Preview.
- [ ] Playback can start, pause/resume, seek, and stop when the app goes into the background.
- [ ] Ordinary text remains editable, and the cursor does not jump when text is changed around a marker.
- [ ] Multiple attachments appear in the correct order and do not play simultaneously.
- [ ] Save, discard, and delete preserve the existing encrypted-audio lifecycle.
- [ ] Plain text does not show a Markdown preview action; Markdown preview remains optional.
- [ ] Existing unit tests, lint, and applicable Android regression tests pass.

## Implementation plan
1. Parse valid, unique attachment IDs using `NoteText.voiceSegments(body)`.
2. Reuse `InlineVoicePlayer` in edit mode, outside `BasicTextField`, so playback does not interfere with IME/cursor behavior.
3. Keep the player list accessible in a scrollable editor layout without fixed proportions that shrink the text field excessively.
4. Reuse active-player and pending-deletion state; preserve existing save/discard semantics.
5. Add regression tests for plain text, Markdown, multiple attachments, malformed markers, and missing audio.
6. Run compilation, unit tests, lint, and supported Android regression tests; record actual outcomes below.

## Validation results
- [x] Android lint — passed in CI run #613 (commit `8602a438`).
- [x] Unit tests — passed in CI run #613 (commit `8602a438`).
- [x] Debug APK build — passed in CI run #613 (commit `8602a438`).
- [x] Minified release APK compilation — passed in CI run #613 (commit `8602a438`).
- [x] Android instrumentation tests compiled — passed in CI run #613; device execution was not performed by this workflow.
- [ ] Manual on-device playback, background behavior, and cursor/save/delete acceptance checks — still required.
