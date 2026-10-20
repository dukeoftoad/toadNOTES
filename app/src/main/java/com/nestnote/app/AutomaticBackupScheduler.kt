package com.nestnote.app

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Owns the unique periodic work request for [AutomaticBackupWorker].
 *
 * Callers flip the persisted flag themselves; this object only manages the schedule.
 * [ExistingPeriodicWorkPolicy.UPDATE] is used so re-enabling never stacks duplicate
 * periodic work — the existing request is replaced in place.
 *
 * The work is local-only: it writes to app-private storage and never touches the
 * network, so no network constraint is required.
 */
object AutomaticBackupScheduler {
    private const val WORK_NAME = "nestnote_automatic_backup"

    /** Enqueues (or updates) the unique daily backup request. Safe to call repeatedly. */
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<AutomaticBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                    .build()
            )
            .build()
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
    }

    /** Cancels the unique daily backup request. Safe to call when nothing is scheduled. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
    }
}