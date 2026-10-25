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

/**
 * A transient media-style notification for the currently playing inline voice note.
 * It deliberately shows only the recording label and elapsed time, never note body text.
 */
internal object VoicePlaybackNotifications {
    private const val CHANNEL_ID = "nestnote_voice_playback_v1"
    private const val CHANNEL_NAME = "Voice-note playback"
    private const val NOTIFICATION_ID = 0x4E455354
    private const val ACTION_TOGGLE = "com.nestnote.app.action.VOICE_PLAYBACK_TOGGLE"
    private const val ACTION_STOP = "com.nestnote.app.action.VOICE_PLAYBACK_STOP"

    internal enum class Action { TOGGLE, STOP }
    @Volatile private var actionHandler: ((Action) -> Unit)? = null
    @Volatile private var activeAttachmentId: String? = null
    @Volatile private var lastSignature: String? = null
    @Volatile private var lastPostedSecond: Int = -1

    fun show(
        context: Context,
        attachmentId: String,
        displayName: String,
        positionMs: Int,
        durationMs: Int,
        paused: Boolean,
        onAction: (Action) -> Unit
    ) {
        activeAttachmentId = attachmentId
        actionHandler = onAction
        ensureChannel(context)
        val safeName = displayName.take(100).ifBlank { "Voice note" }
        val elapsedSecond = (positionMs.coerceAtLeast(0) / 1000)
        val signature = "$safeName|$paused|$elapsedSecond|$durationMs"
        if (signature == lastSignature) return
        // Keep the notification useful without redrawing it on every audio progress callback.
        if (lastSignature != null && safeName == lastSignature?.substringBefore('|') &&
            paused == (lastSignature?.split('|')?.getOrNull(1) == "true") &&
            elapsedSecond == lastPostedSecond
        ) return
        lastSignature = signature
        lastPostedSecond = elapsedSecond

        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }
        val contentIntent = openIntent?.let {
            PendingIntent.getActivity(context, NOTIFICATION_ID, it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        val toggleIntent = PendingIntent.getBroadcast(
            context, 1,
            Intent(context, VoicePlaybackNotificationReceiver::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getBroadcast(
            context, 2,
            Intent(context, VoicePlaybackNotificationReceiver::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val time = "%d:%02d".format(elapsedSecond / 60, elapsedSecond % 60)
        val total = "%d:%02d".format((durationMs.coerceAtLeast(0) / 1000) / 60, (durationMs.coerceAtLeast(0) / 1000) % 60)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(if (paused) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play)
            .setContentTitle(safeName)
            .setContentText(if (paused) "Paused · $time / $total" else "Playing · $time / $total")
            .setSubText("NESTNOTE · Voice note")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_media_play)
                    .setContentTitle("Voice note playback")
                    .setContentText("Audio is playing in NESTNOTE")
                    .build()
            )
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setContentIntent(contentIntent)
            .addAction(
                if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (paused) "Resume" else "Pause",
                toggleIntent
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopIntent)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // Notifications are optional; playback remains usable if permission is denied.
        }
    }

    fun dispatch(context: Context, action: Action) {
        val handler = actionHandler
        if (handler == null) {
            clear(context)
            return
        }
        handler.invoke(action)
    }

    fun clear(context: Context, attachmentId: String? = null) {
        // An inactive sibling player must not clear the active note's notification.
        if (attachmentId != null && activeAttachmentId != attachmentId) return
        activeAttachmentId = null
        actionHandler = null
        lastSignature = null
        lastPostedSecond = -1
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW).apply {
                description = "Playback controls for the voice note currently playing in NESTNOTE"
                setShowBadge(false)
            }
        )
    }

    internal fun actionFromIntent(intent: Intent?): Action? = when (intent?.action) {
        ACTION_TOGGLE -> Action.TOGGLE
        ACTION_STOP -> Action.STOP
        else -> null
    }
}

class VoicePlaybackNotificationReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = VoicePlaybackNotifications.actionFromIntent(intent) ?: return
        VoicePlaybackNotifications.dispatch(context, action)
    }
}
