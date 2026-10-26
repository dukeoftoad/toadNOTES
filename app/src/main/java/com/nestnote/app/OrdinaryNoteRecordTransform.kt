package com.nestnote.app

/**
 * Builds the persisted locked representation for an ordinary note.
 *
 * Key management, encryption, authenticated decryption verification, and persistence remain
 * owned by the calling workflow. This function only maps an already-encrypted payload to fields.
 */
internal object OrdinaryNoteRecordTransform {
    fun lockedRecord(note: NestNote, encryptedPayload: String): NestNote {
        require(!note.privateNote && !note.encrypted) {
            "Only unencrypted ordinary notes can be encrypted here."
        }
        return note.copy(
            title = NoteStore.ENCRYPTED_NOTE_TITLE,
            body = "",
            category = "Encrypted",
            color = 0,
            pinned = false,
            reminder = "",
            background = 0,
            wallpaperUri = "",
            format = "plain",
            encrypted = true,
            encryptedPayload = encryptedPayload
        )
    }
}
