package com.nestnote.app

/**
 * Reconciles encrypted voice attachments on disk against markers in a note body.
 *
 * Two failure modes this addresses:
 *
 *  1. **Marker present, file missing** → handled by [InlineVoicePlayer]'s
 *     `MissingVoiceAttachmentCard`. No reconciler action required; the user can
 *     clean up the dead marker with one tap.
 *  2. **File present, marker missing** → an orphan. Caused by the user manually
 *     editing the raw body in edit mode, by an interrupted save, or by an old
 *     version of the app before markers existed. This class finds and removes
 *     orphans.
 *
 * **Why orphan cleanup is not automatic on every save.**
 *  The editor's `performSave` already handles UI-initiated deletions (see
 *  `pendingVoiceDeletions` in `NoteEditorContent`) with a deferred-delete policy
 *  that survives Cancel/Discard. Automatic orphan cleanup on every save would
 *  re-introduce a data-loss risk if the parent's persistence fails *after* the
 *  editor deletes ciphertext. The reconciler is therefore invoked from a place
 *  the caller controls — usually right after a save has been confirmed by
 *  re-reading the note from storage.
 *
 * **Recommended call sites.**
 *  - After a successful save, when the note is confirmed persisted (see wiring
 *    snippet in the file 5 delivery notes).
 *  - On note open, as a background pass.
 *  - On app start, iterating every note via [pruneAll].
 *  - As an explicit "Clean up storage" action in Settings.
 */
object VoiceAttachmentReconciler {

    /**
     * Returns the sorted IDs of attachments on disk for [noteId] whose marker
     * is absent from [body]. Non-destructive; does not touch the manifest or
     * the filesystem. Safe to call from any thread.
     *
     * A non-positive [noteId] returns an empty list — note IDs are always
     * assigned before voice attachments can be saved, so this only happens in
     * test or programmer-error scenarios.
     */
    fun findOrphans(
        noteId: Long,
        body: String,
        store: VoiceAttachmentStore
    ): List<String> {
        if (noteId <= 0L) return emptyList()
        val onDisk = runCatching { store.list(noteId) }
            .getOrDefault(emptyList())
            .map { it.attachmentId }
        if (onDisk.isEmpty()) return emptyList()
        val anchored = NoteText.voiceMarkers(body).map { it.attachmentId }.toSet()
        return onDisk.filter { it !in anchored }.sorted()
    }

    /**
     * Deletes orphaned attachments (encrypted file + manifest entry) and
     * returns the IDs actually removed.
     *
     * Each deletion is attempted independently inside `runCatching`; a failure
     * to delete one orphan does not prevent the others. Failures are not
     * propagated because orphaned files can never affect correctness of the
     * note — a partial prune is simply retried on the next pass.
     */
    fun pruneOrphans(
        noteId: Long,
        body: String,
        store: VoiceAttachmentStore,
        manifest: VoiceAttachmentManifest
    ): List<String> {
        val orphans = findOrphans(noteId, body, store)
        if (orphans.isEmpty()) return emptyList()
        orphans.forEach { id ->
            runCatching { store.delete(noteId, id) }
            runCatching { manifest.remove(noteId, id) }
        }
        return orphans
    }

    /**
     * Batch prune across many notes. Intended for a Settings maintenance action
     * or an app-start cleanup pass. Returns the total number of orphaned files
     * deleted across all notes.
     */
    fun pruneAll(
        notes: List<NestNote>,
        store: VoiceAttachmentStore,
        manifest: VoiceAttachmentManifest
    ): Int {
        if (notes.isEmpty()) return 0
        return notes.sumOf { note ->
            runCatching {
                pruneOrphans(note.id, note.body, store, manifest).size
            }.getOrDefault(0)
        }
    }
}