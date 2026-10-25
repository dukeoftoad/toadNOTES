# Voice-note backend

This package is the backend boundary for voice-note operations. UI code should depend on
`VoiceNoteRepository` rather than coordinating encrypted payload storage and metadata directly.

## Current storage components

- `VoiceAttachmentStore` (currently in the parent `com.nestnote.app` package): encrypted audio
  payloads, AES-GCM authentication, note/attachment identity binding, size limits, atomic file
  commit, and attachment copy/delete operations.
- `VoiceAttachmentManifest` (currently in the parent package): versioned metadata index, atomic
  writes, and conservative reconciliation.
- `VoiceNoteRepository` (this package): coordinates save/read/delete/list/reconciliation and
  exposes a single backend API for new callers.
- `VoiceNoteImporter` (this package): streams user-selected audio documents through
  `ContentResolver` into the repository, without staging plaintext files or buffering the entire
  source in memory.

Audio payload encryption uses its own Android Keystore alias and is separate from both ordinary
note encryption and Private Vault encryption. A note's encrypted text payload does not implicitly
encrypt or include its audio attachments.

## Import formats and boundaries

The importer recognizes common audio documents by audio MIME type or extension, including WAV,
MP3, FLAC, AAC, M4A, OGG, Opus, AMR, and 3GP. It uses provider metadata for display name, declared
size, and best-effort duration, and applies the existing 25 MiB encrypted-store input limit.
Unknown provider sizes remain safe because the storage layer enforces the limit while streaming.

Importing is not transcoding. The original encoded bytes are retained inside the encrypted
attachment. Whether an imported codec/container can be played depends on Android's decoder support
on the target device; an import success must not be presented as a guarantee of playback support.
Some providers do not expose duration, in which case duration is recorded as zero.

The editor document picker is connected to the importer. Imported encoded files are passed to Android MediaPlayer through an in-memory data source after authenticated decryption; built-in PCM recordings retain the PCM/WAV playback path. When a provider omits a filename extension, the importer infers one from common MIME types or uses an .audio marker so the player does not misclassify the payload as raw PCM. Playback remains dependent on device decoder support. Importing call recordings is supported when the user has a recording file that Android exposes through the document picker. The app does not record, intercept, or extract calls from the phone system.

## Voice-player presentation

The inline voice player supports Compact, Full, Waveform, Minimal, and Transport layouts. Transport adds explicit 10-second rewind/forward controls alongside seeking. Settings provides a custom player-size control from 75% to 150% in 5% increments.
An Android media-style notification appears during active inline playback with the recording label, elapsed/total time, pause/resume, and stop controls. Lock-screen public content hides the recording label. On Android 13+, NESTNOTE requests notification permission contextually on first playback and remembers that it asked; if denied, playback still works and the user can enable notifications in system settings. Current playback remains foreground-only: the existing lifecycle safety rule stops audio when NESTNOTE leaves the foreground. These preferences affect presentation only; they do not resample or rewrite the encrypted audio.
Existing installs retain their previous Compact/Full choice when no newer style preference exists.

## Refactor plan

1. Keep `VoiceNoteRepository` as the only backend boundary for capture, import, attach, read, delete,
   and lifecycle cleanup.
2. Move storage primitives into the `voicenote` package only after all production and test references
   can be migrated together. Do not perform a package-wide rename before the import and failure tests
   establish stable behavior.
3. Add tests for recognized extensions/MIME types, unknown-size streams, size-limit rejection,
   missing providers, duration-unavailable files, metadata commit failure, deletion failure, and
   process interruption during streaming save.
4. Keep the editor document picker connected to `VoiceNoteImporter` and ensure unsupported codecs
   produce a clear playback error without deleting the encrypted attachment.
5. Verify ordinary-note export/backup and restore behavior so voice attachments are never silently
   represented as included in portable note backups.
6. Run exact-head Android CI and device tests before treating the refactor as release-ready.

## Safety invariants

- Never write plaintext audio to disk.
- Never buffer a complete audio file into memory for import or capture.
- Never return plaintext until AES-GCM authentication succeeds.
- Do not delete unindexed ciphertext during recovery; metadata loss must not destroy recordings.
- Do not recreate a missing decryption key for existing ciphertext.
- Do not share encryption keys between voice notes, ordinary notes, and Private Vault.
- Imported call recordings are user-selected files; do not attempt system-call interception.
