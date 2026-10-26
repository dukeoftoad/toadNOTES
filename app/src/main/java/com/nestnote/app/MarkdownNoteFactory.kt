package com.nestnote.app

/**
 * Builds new note records from already-parsed Markdown documents.
 *
 * The caller owns document access, category selection, persistence, and UI feedback.
 * Supplying the clock value makes ID/timestamp allocation deterministic in JVM tests.
 */
internal object MarkdownNoteFactory {
    fun create(
        parsedNotes: List<MarkdownNoteParser.ParsedNote>,
        existingNotes: List<NestNote>,
        category: String,
        now: Long
    ): List<NestNote> {
        val usedNotes = existingNotes.toMutableList()
        return parsedNotes.mapIndexed { index, parsed ->
            val timestamp = now + index
            NestNote(
                id = NoteStore.allocateNoteId(usedNotes, timestamp),
                title = parsed.title,
                body = parsed.body,
                category = category,
                updated = timestamp,
                created = timestamp
            ).also { usedNotes += it }
        }
    }
}
