# Changelog

All notable changes to NESTNOTE are documented here.

## [1.3.11] - Current branch record (2026-10-22)

This is the current v1.3.11 release record for the branch prepared by PR #76, covering note-editor and Markdown work associated with [issue #61](https://github.com/dukeoftoad/toadNOTES/issues/61). The codebase/documentation entry is current for this PR; the public release tag and signed APK are separate publication steps and are not claimed to exist yet.

### Fixed
- Corrected editor text contrast selection for dark surfaces by using surface luminance and theme-provided foreground colors.
- Improved Markdown blockquote and horizontal-rule preview rendering while keeping raw HTML-like content inert.
- Preserved meaningful whitespace, indentation, trailing-newline semantics, and LF/CRLF/lone-CR line endings in text conversion and list operations.
- Corrected selection remapping after line edits to better preserve cursor and reversed-selection positions.
- Hardened inline-format toggles and checklist/bullet transitions, including indentation, Enter behavior, and mixed line endings.
- Restricted actionable Markdown preview links to `http`, `https`, and `mailto` schemes; unsupported schemes are removed from actionable preview content.
- Improved accessibility touch-target sizing and added standalone Markdown heading semantics for preview segments.

### Markdown renderer — PR #74 (merged 2026-10-22)
- Replace the regex-based inline/block Markdown preprocessor with **Markwon**, using its CommonMark-based Android rendering pipeline.
- Add Markwon task-list and strikethrough extensions for richer Markdown preview support.
- Pass the current Android context into preview rendering.
- Keep the original note source unchanged during preview, disable raw HTML rendering, and restrict actionable links to `http`, `https`, and `mailto` schemes.
- Update preview regression tests to assert rendered behavior rather than parser-specific spans.

### Link insertion — PR #74 (merged 2026-10-22)
- Escape backslashes and both square-bracket delimiters in selected link labels in a single pass.
- Add coverage for Unicode labels, punctuation, empty selections, document boundaries, reversed selections, escaping, and cursor placement inside the URL placeholder.

### Tests and engineering
- Added regression coverage for Markdown rendering, formatting toggles, list/checklist transitions, link escaping and cursor placement, line-ending preservation, and safe-link handling.
- Added draft persistence/isolation and undo/redo regression coverage.
- Added Compose editor integration coverage for save/recovery, discard/reopen, preview/edit switching, formatting actions, link insertion, note switching, and Activity recreation.
- Added CI workflow coverage to execute Android emulator UI tests on pull requests.

### Known validation gaps
- PR #74's latest CI status and integration behavior must be confirmed before treating its Markwon replacement and link-insertion changes as verified.
- Do not consider every item in issue #61 complete solely from this changelog. Check the issue and linked pull requests for exact scope and evidence.
- Confirm CI passes on the final release commit, including unit tests, lint, release compilation, and the configured emulator/UI-test jobs.
- Verify production signing and the exact release APK certificate; do not distribute unsigned or inspection-only artifacts.
- Record any remaining physical-device checks, including TalkBack semantics, large fonts, keyboard/insets, theme contrast, and process-death recovery, before declaring release readiness.
