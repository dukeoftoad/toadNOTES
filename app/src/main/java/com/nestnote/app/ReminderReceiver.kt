package com.nestnote.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Fires when a scheduled reminder alarm goes off. Posts a notification and does
 * nothing else — the alarm is one-shot, so a re-scheduled alarm is needed if the
 * user wants a recurring reminder.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = ReminderScheduler.noteIdFromIntent(intent)
        if (noteId <= 0L) return
        val title = ReminderScheduler.noteTitleFromIntent(intent)
            .ifBlank { "Note reminder" }
        runCatching { ReminderNotifications.show(context, noteId, title) }
    }
}