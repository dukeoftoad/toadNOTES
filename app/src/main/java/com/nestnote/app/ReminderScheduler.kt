package com.nestnote.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

/**
 * Schedules and cancels per-note reminder alarms.
 *
 * PendingIntent uniqueness comes from the intent's `action` string, which embeds
 * the note ID. This avoids the request-code collision problem that a plain modulo
 * scheme would have for near-sequential timestamps.
 *
 * On Android 12+ without SCHEDULE_EXACT_ALARM permission, falls back to inexact
 * `setAndAllowWhileIdle` — the OS may delay the alarm by up to several minutes
 * but it will still fire. That's preferable to refusing to schedule at all.
 */
object ReminderScheduler {
    private const val EXTRA_NOTE_ID = "note_id"
    private const val EXTRA_NOTE_TITLE = "note_title"

    fun schedule(context: Context, note: NestNote) {
        if (note.reminder.isBlank()) return
        val triggerAt = NoteTextParsing.parseReminderOrNull(note.reminder)?.timeInMillis ?: return
        if (triggerAt <= System.currentTimeMillis()) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        // FLAG_UPDATE_CURRENT always creates or updates the PendingIntent, so this
        // shouldn't return null. The elvis is defensive and satisfies the non-null
        // contract required by the AlarmManager APIs below.
        val pendingIntent = buildPendingIntent(
            context, note.id, note.title, PendingIntent.FLAG_UPDATE_CURRENT
        ) ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            runCatching {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    fun cancel(context: Context, noteId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(
            context, noteId, "",
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_NO_CREATE
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    fun rescheduleAll(context: Context, notes: List<NestNote>) {
        notes.filter { it.reminder.isNotBlank() }.forEach { schedule(context, it) }
    }

    private fun buildPendingIntent(
        context: Context,
        noteId: Long,
        noteTitle: String,
        extraFlags: Int
    ): PendingIntent? {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.nestnote.app.REMINDER.$noteId"
            putExtra(EXTRA_NOTE_ID, noteId)
            putExtra(EXTRA_NOTE_TITLE, noteTitle)
        }
        val flags = extraFlags or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, 0, intent, flags)
    }

    internal fun noteIdFromIntent(intent: Intent): Long = intent.getLongExtra(EXTRA_NOTE_ID, 0L)
    internal fun noteTitleFromIntent(intent: Intent): String =
        intent.getStringExtra(EXTRA_NOTE_TITLE).orEmpty()
}