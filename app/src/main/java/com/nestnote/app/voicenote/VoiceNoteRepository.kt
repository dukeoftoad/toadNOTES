package com.nestnote.app.voicenote

import android.content.Context
import com.nestnote.app.VoiceAttachment
import com.nestnote.app.VoiceAttachmentManifest
import com.nestnote.app.VoiceAttachmentMetadata
import com.nestnote.app.VoiceAttachmentStore
import java.io.IOException
import java.io.InputStream

/**
 * Voice-note backend boundary for the editor and future voice-note UI.
 *
 * This repository coordinates encrypted payload storage with its durable metadata index.
 * The existing storage implementations remain the source of truth for encryption and atomic
 * file handling; this class keeps callers from coordinating those two components themselves.
 *
 * Payloads are encrypted by VoiceAttachmentStore using its dedicated Android Keystore key.
 * They are deliberately independent of ordinary-note encryption and Private Vault keys.
 */
class VoiceNoteRepository private constructor(
    private val store: VoiceAttachmentStore,
    private val manifest: VoiceAttachmentManifest,
    @Suppress("UNUSED_PARAMETER") testSeam: Unit
) {
    constructor(context: Context) : this(
        VoiceAttachmentStore(context),
        VoiceAttachmentManifest(context),
        Unit
    )

    /** Test seam: production callers should use the Context constructor. */
    internal constructor(store: VoiceAttachmentStore, manifest: VoiceAttachmentManifest) :
        this(store, manifest, Unit)

    /** Returns only manifest entries whose encrypted payloads still exist and match the index. */
    @Throws(IOException::class)
    fun list(noteId: Long): List<VoiceAttachmentMetadata> = manifest.list(noteId, store)

    /**
     * Lists encrypted payloads even when metadata is missing, so startup recovery can attach
     * legacy/orphaned files to their existing inline markers without exposing audio contents.
     */
    fun listPayloads(noteId: Long): List<VoiceAttachment> = store.list(noteId)

    /**
     * Persist encrypted audio for the review step without publishing it as an attached recording.
     * The payload remains encrypted and discoverable for recovery until explicitly attached or discarded.
     */
    @Throws(IOException::class)
    fun saveForReview(noteId: Long, attachmentId: String, input: InputStream): VoiceAttachment =
        store.save(noteId, attachmentId, input)

    /** Publish metadata for a successfully saved review payload after the user chooses Attach. */
    @Throws(IOException::class)
    fun attachExisting(
        noteId: Long,
        attachmentId: String,
        createdAtMillis: Long,
        durationMillis: Long,
        displayName: String
    ): VoiceAttachmentMetadata {
        val payload = store.list(noteId).firstOrNull { it.attachmentId == attachmentId }
            ?: throw IOException("Attachment payload is missing; metadata was not committed")
        val metadata = VoiceAttachmentMetadata(
            noteId = noteId,
            attachmentId = attachmentId,
            createdAtMillis = createdAtMillis,
            durationMillis = durationMillis,
            encryptedSizeBytes = payload.encryptedSizeBytes,
            displayName = displayName
        )
        manifest.put(metadata, store)
        return metadata
    }

    /** Remove an unattached review payload without changing unrelated manifest entries. */
    @Throws(IOException::class)
    fun deletePayload(noteId: Long, attachmentId: String) {
        if (!store.delete(noteId, attachmentId)) {
            throw IOException("Voice recording payload could not be deleted")
        }
    }

    /**
     * Encrypt and persist the recording first, then publish metadata. If metadata publication
     * fails, remove only the newly-created payload so no unindexed partial save is left behind.
     */
    @Throws(IOException::class)
    fun save(
        noteId: Long,
        attachmentId: String,
        input: InputStream,
        createdAtMillis: Long,
        durationMillis: Long,
        displayName: String
    ): VoiceAttachmentMetadata {
        val saved: VoiceAttachment = store.save(noteId, attachmentId, input)
        val metadata = VoiceAttachmentMetadata(
            noteId = noteId,
            attachmentId = saved.attachmentId,
            createdAtMillis = createdAtMillis,
            durationMillis = durationMillis,
            encryptedSizeBytes = saved.encryptedSizeBytes,
            displayName = displayName
        )
        try {
            manifest.put(metadata, store)
            return metadata
        } catch (error: Exception) {
            // Do not leave a payload from a save whose metadata commit failed.
            // If cleanup itself fails, retain the original exception and its context.
            runCatching { store.delete(noteId, saved.attachmentId) }
            throw IOException("Voice recording could not be committed", error)
        }
    }

    /** Authentication is verified by AES-GCM before plaintext bytes are returned. */
    @Throws(IOException::class)
    fun read(noteId: Long, attachmentId: String): ByteArray = store.read(noteId, attachmentId)

    /**
     * Delete ciphertext first; a failed payload deletion must not hide the recording from the
     * metadata index. A stale metadata entry is safe and can be reconciled on the next list.
     */
    @Throws(IOException::class)
    fun delete(noteId: Long, attachmentId: String) {
        if (!store.delete(noteId, attachmentId)) {
            throw IOException("Voice recording could not be deleted")
        }
        manifest.remove(noteId, attachmentId)
    }

    /** Removes stale index rows only. It intentionally does not delete unindexed audio files. */
    @Throws(IOException::class)
    fun reconcile(liveNoteIds: Set<Long>): Int = manifest.reconcile(store, liveNoteIds)

    /** Use only after the owning note has been durably deleted or a new draft is discarded. */
    @Throws(IOException::class)
    fun deleteAllForNote(noteId: Long) {
        if (!store.deleteAll(noteId)) throw IOException("Voice recordings could not be deleted")
        manifest.removeNote(noteId)
    }

    companion object {
        const val MAX_RECORDING_BYTES: Int = 25 * 1024 * 1024
        const val MAX_IMPORT_BYTES: Int = 100 * 1024 * 1024
    }
}
