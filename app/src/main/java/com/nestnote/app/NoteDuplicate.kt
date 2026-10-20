package com.nestnote.app

/**
 * Produces a copy of a note for the "Duplicate" action.
 *
 * The duplicate:
 *  - gets a fresh collision-free ID via [NoteStore.allocateNoteId]
 *  - gets new `created` and `updated` timestamps
 *  - is never archived
 *  - keeps body, category, color, background, and wallpaper URI
 *  - clears the reminder by default (so it doesn't fire a duplicate alarm);
 *    pass `keepReminder = true` if you want to preserve it
 *  - keeps or clears `pinned` based on the [keepPinned] parameter (default: false)
 */
object NoteDuplicate {
    fun duplicate(
        source: NestNote,
        existing: Collection<NestNote>,
        keepReminder: Boolean = false,
        keepPinned: Boolean = false,
        nowMillis: Long = System.currentTimeMillis()
    ): NestNote {
        return source.copy(
            id = NoteStore.allocateNoteId(existing, nowMillis),
            updated = nowMillis,
            created = nowMillis,
            archived = false,
            pinned = if (keepPinned) source.pinned else false,
            reminder = if (keepReminder) source.reminder else ""
        )
    }
}