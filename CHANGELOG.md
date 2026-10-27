# Changelog

All notable changes to NESTNOTE are documented here. The current release candidate is **1.4.7**.

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