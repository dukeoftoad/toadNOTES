package com.nestnote.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-registers reminder alarms after a device reboot. AlarmManager alarms do not
 * survive reboot; without this receiver, all reminders would silently disappear.
 *
 * `goAsync()` lets us do I/O off the main thread without missing the
 * BroadcastReceiver window. The work is bounded (read notes → reschedule alarms)
 * and completes well under the 10-second receiver deadline.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        Thread({
            try {
                val result = NoteStore(context).loadSafely()
                if (!result.recoveryRequired) {
                    ReminderScheduler.rescheduleAll(context, result.notes)
                }
            } catch (_: Exception) {
                // Boot recovery is best-effort. A failure here just means reminders
                // will be rescheduled on the next app launch.
            } finally {
                pendingResult.finish()
            }
        }, "nestnote-boot-reminders").start()
    }
}