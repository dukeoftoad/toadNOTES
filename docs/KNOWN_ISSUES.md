# NESTNOTE — Known Issues

Living document. New issues are appended as they're discovered; when fixed,
either strike through in place or move to the "Resolved" section at the bottom
with the commit/PR reference.

**Last updated:** following the fingerprint folder-access and 100 MiB voice-import fix batch.

---

## Open issues

_No open issues tracked in this file right now. Device-only verification gaps are
tracked in `docs/WORKLOG.md` and `docs/ROADMAP.md` release gates._

---

## Resolved issues

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
