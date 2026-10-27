# NESTNOTE — Known Issues

Living document. New issues are appended as they're discovered; when fixed,
either strike through in place or move to the "Resolved" section at the bottom
with the commit/PR reference.

**Last updated:** following the imported-voice-marker fix batch (dash-less UUID tolerance, missing-notes noise).

---

## Open issues

_No open issues tracked in this file right now. Device-only verification gaps are
tracked in `docs/WORKLOG.md` and `docs/ROADMAP.md` release gates._

---

## Resolved issues

### Imported voice notes showed "Voice note unavailable" (RESOLVED)

**Severity:** high (imported audio unplayable from the note; latent data-loss path)
**Files:** `NoteText.kt`, `NoteEditorSupport.kt`, `voicenote/VoiceNoteImporter.kt`

**Symptom:** Every *imported* audio attachment (editor → import, e.g. a dialer
recording) rendered as a "Voice note unavailable" chip with no inline player,
and the footer diagnostic printed `Missing notes: voice:87f8742b89c34486a8d281e5dbace545`.
In-app *recordings* were unaffected.

**Root cause:** Two id formats coexisted. `VoiceRecordingCoordinator` writes the
canonical dashed UUID; `VoiceNoteImporter` wrote the same UUID with the dashes
stripped (32 hex chars). Every marker parser required the dashed form, so
dash-less markers were treated as malformed payloads:

- `NoteText.voiceMarkers`/`voiceSegments` returned nothing playable for them.
- The editor chip transformation labeled them "Voice note unavailable".
- `VoiceAttachmentReconciler.findOrphans` would have flagged their files as
  orphans (data loss) as soon as prune wiring is enabled.
- `noteLinkTitles` fed the raw `voice:<id>` text into the "Missing notes" list.

**Fix:** Marker parsing now accepts both UUID forms (dashed canonical and
dash-less 32-hex) in `NoteText`, the editor chip, and the link parser; arbitrary
or wrong-length payloads stay hidden. New imports generate the canonical dashed
UUID so both paths converge. Regression tests pin dash-less recognition,
malformed-payload hiding, and the link-parser exclusion.

### S3 — Spaced searches matched across field boundaries (RESOLVED)

**Severity:** low
**File:** `NoteFilters.kt` (also tracked as S3 in `docs/P1_QUALITY_AUDIT.md`)

**Symptom:** The free-text query ran against a single concatenated
`title + " " + body + " " + category` string, so a query with a space could match
across the boundary — "plan 1." matched a note titled "Project Plan" whose body
started "1. Finalize…" even though the phrase existed in no single field.

**Fix:** The query text is now matched against title, body, and category
separately; a multi-word query must appear as a phrase inside one field. The
query-language operators (`is:`, `has:`, `category:`, `in:`, `after:`,
`before:`) are unchanged. Regression tests pin the boundary behavior and query
trimming.

### S6 — Editor could not restore archived notes; Pin offered on archived notes (RESOLVED)

**Severity:** low/medium
**Files:** `NoteEditorSections.kt`, `NoteEditor.kt`, `NoteCard.kt`
(also tracked as S6 in `docs/P1_QUALITY_AUDIT.md`)

**Symptom:** The editor's overflow menu offered "Archive" even when the note was
already archived (a no-op) and provided no restore path from the editor. Both the
editor menu and the note-card menu offered "Pin note" on archived notes, but
archiving forces `pinned=false`, so the toggle silently did not stick.

**Fix:** The editor menu now offers "Restore from archive" (unarchive icon) for
archived notes and "Archive" otherwise; the Pin action is hidden while a note is
archived in both menus and returns after restore.

### Backup import turned JSON `null` fields into literal "null" text (RESOLVED)

**Severity:** low
**File:** `NoteStore.kt`

**Symptom:** `org.json`'s `optString` returns the literal string "null" for an
explicit JSON `null` (with or without a fallback), so a hand-edited or
third-party backup with `{"title": null, "body": null, …}` imported notes whose
title, body, category, reminder, wallpaper URI, format, and encrypted payload
were the four characters `null`.

**Fix:** All text fields in `NoteStore.decode` (and the archive-envelope format
check) go through an `optText` helper that treats `JSONObject.NULL` as missing,
returning the default value instead. Regression test
`decodeTreatsExplicitJsonNullTextFieldsAsMissingNotLiteralNull` pins the behavior.

### OI-1 — Notification permission denial is silent (RESOLVED)

**Severity:** low (UX gap)
**Files:** `app/src/main/java/com/nestnote/app/NoteEditor.kt`

**Symptom:** When the user first opened the reminder picker on Android 13+, the
app requested `POST_NOTIFICATIONS`. If the user denied, reminders still saved and
`AlarmManager` still fired at the scheduled time — but `NotificationManagerCompat.notify()`
silently no-oped. The user never learned their reminders wouldn't be delivered.

**Fix:** The permission launcher result is now tracked. When notifications are
denied, the reminder time picker shows an inline warning: the reminder will be
saved but will not pop up, with guidance to enable NESTNOTE notifications in
system settings. The warning clears when permission is granted on a later
reminder pick.

### OI-2 — `LocalClipboardManager` deprecation (RESOLVED)

**Severity:** trivial
**File:** `app/src/main/java/com/nestnote/app/AboutScreen.kt`

**Symptom:** Compile warning about the deprecated `LocalClipboardManager`.

**Fix:** Suppressed with a documented rationale at the single call site; the
replacement (`LocalClipboard`) requires a composable-scope receiver that the
About screen's copy helper does not otherwise need. Re-evaluate on the next
Compose bump.

### 100 MiB voice import / playback OOM (RESOLVED)

**Severity:** high (crash)
**Files:** `VoiceAttachmentStore.kt`, `AndroidManifest.xml`

**Symptom:** Imported audio up to the 100 MiB limit could be *saved* (the save
path streams), but *reading* it for playback buffered the encrypted file through
a growing `ByteArrayOutputStream`, then copied it twice more (`toByteArray()` +
`copyOfRange`) before decryption — a peak of roughly 4× the file size
(~400 MiB for a 100 MiB import). On default app heaps this crashed with
`OutOfMemoryError` as soon as the user played a large import.

**Fix:**
- `readLocked` now reads into an exactly-sized buffer and decrypts directly
  from it (`cipher.doFinal(payload, HEADER_BYTES, length)`), removing both
  full-file copies. Peak memory is ~2× the payload.
- `android:largeHeap="true"` declared in the manifest: encrypted voice
  attachments are a legitimate large-heap use case.
- Store limit errors now say "100 MiB" explicitly.

### Audio picker hid some recordings (RESOLVED)

**Severity:** medium
**File:** `VoiceNoteAudio.kt`

**Symptom:** The editor's import button offered only `audio/*`, so dialer
recordings whose provider reports `application/ogg` or `application/x-flac`
were not selectable in the system picker.

**Fix:** The picker now also offers those MIME types; `VoiceAudioImportPolicy`
still validates every selection before import.

### Failed call-recording imports could not be dismissed (RESOLVED)

**Severity:** medium
**File:** `CallRecordingsScreen.kt`

**Symptom:** A failed or stuck-pending import stayed in the list forever: the
only action was "Retry import", which failed again for permanent errors (e.g.
oversize), and every folder scan re-enqueued it.

**Fix:** Failed/pending rows now offer "Dismiss", which adds the source URI to
the ignored-sources set (the same mechanism "Delete copy" uses) and removes the
list entry.

### Fingerprint unlock was not offered for folder management (RESOLVED)

**Severity:** enhancement
**File:** `CallRecordingsScreen.kt`

**Symptom:** Fingerprint unlock existed for opening the recordings screen, but
enabling/disabling auto-import and changing the source folder required the
vault password every time.

**Fix:** The "Confirm Private Vault access" dialog now offers "Use fingerprint
instead" when strong biometrics are enrolled. Password and fingerprint paths
converge in one `runVaultAction` handler so both authenticate identically
(CryptoObject-bound key unwrap before any setting changes).
