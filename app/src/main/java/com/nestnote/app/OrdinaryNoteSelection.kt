package com.nestnote.app

/**
 * Pure selection and replacement operations for ordinary-note encryption actions.
 *
 * Cryptographic operations and persistence stay in NestNoteApp; this helper only determines
 * which records are eligible and applies already-verified replacements to the original list.
 */
internal object OrdinaryNoteSelection {
    fun eligibleForEncryption(notes: List<NestNote>): List<NestNote> =
        notes.filter { !it.privateNote && !it.encrypted }

    fun forEncryption(notes: List<NestNote>, selectedIds: Set<Long>): List<NestNote> =
        eligibleForEncryption(notes).filter { it.id in selectedIds }

    fun forDecryption(notes: List<NestNote>, selectedIds: Set<Long>): List<NestNote> =
        notes.filter { it.id in selectedIds && it.encrypted && !it.privateNote }

    fun applyReplacements(
        notes: List<NestNote>,
        replacements: Map<Long, NestNote>
    ): List<NestNote> = notes.map { replacements[it.id] ?: it }
}
