# Changelog

All notable changes to NESTNOTE are documented here. The current release candidate is **1.4.9**.

## [Unreleased] — Known-bug fixes

### Fixed
- **Imported voice notes no longer show "Voice note unavailable".** Audio imported into a note (e.g. a dialer recording) carried a dash-less attachment id, while every marker parser required the dashed UUID form — so imported attachments rendered as unavailable chips with no player, the footer printed `Missing notes: voice:…`, and the (dormant) orphan reconciler would have treated their files as deletable. Marker parsing now accepts both UUID forms, the "Missing notes" diagnostic no longer lists attachment markers as note links, and new imports use the canonical dashed id.
- **Spaced searches no longer match across field boundaries.** The dashboard query ran against a concatenated `title + body + category` string, so e.g. `plan 1.` matched a note titled "Project Plan" with a body starting "1. Finalize…" even though the phrase exists in no single field. The free-text query is now matched against the title, body, and category separately (multi-word queries match a phrase within one field; `is:`/`has:`/`category:`/`after:`/`before:` operators are unchanged).
- **Archived notes can be restored from the editor.** The editor's ⋮ menu offered "Archive" even for already-archived notes and provided no way back; it now offers "Restore from archive" (with an unarchive icon) for archived notes, matching the note-card menu.
- **Pinning is no longer offered on archived notes.** Archiving forces `pinned=false`, so the Pin action on an archived card or editor menu silently did not stick; it is hidden until the note is restored.
- **Backups with JSON `null` text fields no longer import literal "null" content.** `org.json`'s `optString` returns the string "null" for an explicit JSON `null`, so hand-edited or third-party backups imported notes whose title/body/category/reminder were the four characters `null`. Text fields now treat JSON `null` as missing (empty or default value), with a regression test.
- **Markdown preview no longer merges the editor's list lines or renumbers restarted lists.** The editor stores list state as literal glyphs (`• `, `☑ `, `☐ `) and relies on visible line breaks, but the preview fed the raw body to the CommonMark renderer: consecutive glyph lines collapsed onto one line (`• a • b`), the checkbox line lost its checked state and was absorbed into the preceding numbered item, and a numbered list that restarted (`3.` then `1.`) was silently renumbered as a continuation (`4.`, `5.`). The preview now applies the same glyph conversion Markdown export already used (`MarkdownCompat`), renders single newlines as line breaks, and starts a new list when the numbering restarts or skips, preserving the note's own numbers. Fenced code blocks are left byte-identical and the stored body is never rewritten.

### Fixed (tests)
- Regression coverage added: cross-field search boundary (`NoteFiltersTest`), query trimming, JSON-null import sanitization (`NoteStoreTest`), dash-less voice-marker recognition and malformed-payload hiding (`NoteTextTest`), voice-marker exclusion from note links (`NoteLinkSupportTest`), and preview list-glyph adaptation with the exact reported note body (`MarkdownCompatTest`, `MarkdownPreviewTest`).

## [1.4.9] — Settings polish, Markdown format fixes, voice-import reliability

### Added
- **Fingerprint unlock for call-recording folder management:** the "Confirm Private Vault access" dialog (enable/disable auto-import, change source folder) now offers "Use fingerprint instead" when strong biometrics are enrolled. Both authentication paths converge on one action handler; the folder only changes after the CryptoObject-bound vault key unwraps successfully.
- **Settings search feedback:** the search field reports how many sections match the query (or suggests a shorter keyword), and the list always returns to the top when a query is active instead of landing on blank space after scrolling.
- **Settings version footer:** the bottom of Settings shows the installed version and code, read from the package manager.

### Fixed
- **100 MiB voice imports no longer crash on playback.** Reading an imported recording buffered it through a growing `ByteArrayOutputStream` plus two full-file copies before decryption (~4× file size in memory, `OutOfMemoryError` on default heaps). It now reads into an exactly-sized buffer and decrypts in place (~2× peak), and the manifest declares `android:largeHeap` for the encrypted-audio use case.
- **Italic no longer destroys intraword underscores.** Selecting a word inside `snake_case_word` and applying italic used to delete both underscores; underscore runs are now never unwrapped inside words (CommonMark intraword rule), and the Italic button uses asterisk markers, which are safe inside words and render correctly in preview.
- **Strikethrough and inline code on plain notes now switch the note to Markdown** like Bold and Italic already did, instead of inserting literal `~~`/backtick characters that never render.
- **Link-to-note on a plain note converts it to Markdown** through the same shared path (with the confirmation toast and preview reset) instead of silently flipping the format flag.
- **Imported audio is now selectable in the editor picker.** Dialer recordings reported as `application/ogg` or `application/x-flac` were hidden by the audio-only filter; the picker offers them now while import validation stays strict.
- **Failed call-recording imports can be dismissed.** Failed or stuck-pending sources previously stayed in the list forever with retry as the only action; they now offer "Dismiss", which stops future scans from re-importing them.
- **Reminder notification denial is no longer silent (OI-1).** When notifications are denied, the reminder time picker warns that the reminder will save but not pop up, with guidance to enable notifications in system settings.

## [1.4.7] — Performance & interface refresh

### Performance (frame skipping eliminated at the source)
- The theme `ColorScheme` is now cached and only rebuilt when the theme actually changes. Previously a fresh scheme (dynamic-colour extraction, accent blending, AMOLED overlay) was allocated on every recomposition — including every keystroke in dashboard search — forcing the whole UI tree to recompose.
- Note wallpapers decode off the main thread with power-of-two downsampling and a shared LRU cache. Previously camera-resolution images were decoded synchronously inside composition (multi-hundred-millisecond freezes per card, repeated while scrolling).
- Editor-derived data (voice-marker ids, interactive checklist) now refreshes through a short debounce instead of five full-body O(n) scans per keystroke; the linked-note title index is cached on the note list instead of being rebuilt while typing; word counting is a single allocation-free pass.
- Voice playback notifications refresh on a one-second loop instead of restarting a coroutine (with binder IPC) ten times per second; idle voice players no longer run twelve always-on frame-rate animations; empty states and settings previews animate only when motion is active and allowed.
- Draft flushes no longer run encoding + fsync on the main thread while the editor close animation plays (ON_STOP keeps the synchronous write as a process-death safeguard).
- Dashboard statistics (pinned/archived/public/private counts, selection set, fallback note id, editor default format) are computed once per data change instead of several full-list scans per recomposition; dashboard root no longer wraps the whole note list in `animateContentSize`; note cards share one date formatter; lazy lists declare `contentType`.

### Interface
- Complete Material 3 colour foundation: `surfaceContainerHighest`, `tertiary`, `tertiaryContainer`, and `surfaceDim`/`surfaceBright` are now defined for the warm light/dark schemes, removing default lavender/mauve leaks across settings, dialogs, and empty states.
- Accent palettes generate proper opaque tonal containers (no alpha compositing), with per-palette dark-theme `onPrimary` colors that follow the selected hue.
- Dark mode is a first-class skin: 15 deep-toned note tints matching the pastel palette, dark-aware note cards with adaptive ink, and a note-coloured editor canvas.
- Note cards: tonal category chip, gradient wallpaper scrim for reliable text contrast, refined checklist progress bar.
- Reminders use Material 3 date and time pickers instead of framework dialogs (theme- and AMOLED-aware).
- Dashboard hero shows greeting, active/pinned stat pills, and a filled create button; search field is a pill with autofocus and animated reveal; empty states offer a primary "Create a note" action.
- All remaining ad-hoc tweens route through the shared motion tokens, so every transition honours the system "Remove animations" preference.

## [1.4.6] — Release candidate

This version updates release metadata to 1.4.6 and carries forward the current feature set. Publish only after exact-tag CI, signing, and release gates are verified.

### Included capabilities
- Native Markwon Markdown table rendering and automatic pipe-table alignment.
- Private Vault biometric enrollment and CryptoObject-based unlock path, with password recovery; physical-device validation remains a release check.
- Bounded SAF backup verification reads.

## [1.4.5] — Previous release baseline

Version 1.4.5 is the previous release baseline. Consult [GitHub Releases](https://github.com/dukeoftoad/toadNOTES/releases) for the authoritative published release asset and metadata.

### Product capabilities
- Markdown pipe tables render as tables in note preview; pasted Markdown tables are automatically column-aligned, while fenced code and ordinary pipe text remain unchanged.
- Local-first note creation, editing, search, categories, pinning, archive/restore, sorting, and configurable Home layout.
- Editor and saved-note reader zoom from 50% to 200% in 5% steps; zoom is presentation-only.
- Transient read-only external Markdown viewer with separate configurable zoom.
- Offline Text Toolbox with text statistics, reading-time estimate, case conversion, whitespace cleanup, copy, and clear.
- Private Vault with password access and optional biometric flow; key invalidation and recovery must fail safely.
- Validated JSON backup/restore with explicit plaintext-export warning; audio attachments are excluded.

## Historical notes: 1.3.11 and earlier

Older references to v1.3.10 as latest stable or v1.3.11 as current candidate are historical and must not be used as the current-version statement.

### Editor and Markdown reliability
- Markwon/CommonMark rendering with task-list and strikethrough extensions.
- Raw HTML disabled in preview; actionable links restricted to http, https, and mailto.
- Preserve whitespace, indentation, line endings, cursor/selection, and checklist/bullet behavior during editing.
- External Markdown opens in a transient read-only viewer and does not silently import into saved notes.

### Validation and release policy
- Validate the exact release commit with CI and configured test jobs.
- Compiling instrumentation tests is not proof of device execution.
- Verify production signing and the exact APK certificate before distribution.
- Record physical-device biometric enrollment/unlock, key invalidation/password fallback, TalkBack, large fonts, keyboard/insets, migration, and process-death recovery.
- Do not claim tests, signing, or release success without evidence for the exact commit/artifact.