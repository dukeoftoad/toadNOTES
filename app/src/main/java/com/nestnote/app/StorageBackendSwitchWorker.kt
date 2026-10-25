package com.nestnote.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Data
import java.util.concurrent.TimeUnit

/**
 * Executes a user-confirmed storage-backend change after a five-hour cooling-off period.
 * The worker reloads from the active backend at execution time rather than trusting stale UI state.
 */
internal class StorageBackendSwitchWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val targetName = inputData.getString(KEY_TARGET) ?: return Result.failure()
        val target = runCatching { NoteStorageBackend.valueOf(targetName) }.getOrNull()
            ?: return Result.failure()
        return runCatching {
            val store = NoteStore(applicationContext)
            val current = store.loadSafely()
            if (current.recoveryRequired) return Result.failure()
            val switched = store.switchBackend(target, current.notes)
            if (switched.recoveryRequired) Result.failure() else Result.success()
        }.getOrElse { Result.failure() }
    }

    companion object {
        private const val WORK_NAME = "nestnote_delayed_storage_backend_switch"
        private const val KEY_TARGET = "target_backend"
        const val DELAY_HOURS = 5L

        fun schedule(context: Context, target: NoteStorageBackend) {
            val request = OneTimeWorkRequestBuilder<StorageBackendSwitchWorker>()
                .setInitialDelay(DELAY_HOURS, TimeUnit.HOURS)
                .setInputData(Data.Builder().putString(KEY_TARGET, target.name).build())
                .build()
            WorkManager.getInstance(context.applicationContext)
                .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context.applicationContext).cancelUniqueWork(WORK_NAME)
        }
    }
}
