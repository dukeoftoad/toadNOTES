# Changelog

All notable changes to NESTNOTE are documented here. The current release candidate is **1.4.6**.

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