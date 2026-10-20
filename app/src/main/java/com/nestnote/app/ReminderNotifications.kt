package com.nestnote.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/**
 * Notification channel + builder for note reminders.
 *
 * The channel is created lazily on the first notification; existing channels are
 * left untouched so user preferences (importance, sound) survive app upgrades.
 * The channel ID is versioned (`_v1`) so a future importance change can create a
 * new channel without fighting the OS's immutability rules.
 */
object ReminderNotifications {
    private const val CHANNEL_ID = "nestnote_reminders_v1"
    private const val CHANNEL_NAME = "Note reminders"
    private const val CHANNEL_DESCRIPTION = "Notifications for scheduled note reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESCRIPTION
        }
        manager.createNotificationChannel(channel)
    }

    fun show(context: Context, noteId: Long, title: String) {
        ensureChannel(context)
        val openIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            ?: return
        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Your note reminder")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(noteId.toInt(), notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS not granted — nothing to do.
        }
    }
}