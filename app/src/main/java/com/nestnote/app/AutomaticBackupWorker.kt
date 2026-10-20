package com.nestnote.app

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

/**
 * Periodic local backup. Android may defer this work under Doze, battery restrictions,
 * or storage pressure; it is best-effort rather than a clock-exact schedule.
 */
class AutomaticBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("nestnote_preferences", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("automatic_backup_enabled", false)) return Result.success()

        return try {
            val loaded = NoteStore(applicationContext).loadSafely()
            if (loaded.recoveryRequired) {
                prefs.edit().putLong("automatic_backup_last_run", System.currentTimeMillis())
                    .putString("automatic_backup_last_status", "Failed: notes need recovery").apply()
                return Result.failure()
            }
            val backupDir = File(applicationContext.filesDir, "automatic-backups")
            if (!backupDir.exists() && !backupDir.mkdirs()) error("Cannot create backup directory")
            val target = File(backupDir, "latest.json")
            val temp = File(backupDir, "latest.json.tmp")
            temp.writeText(NoteStore.encodeBackup(loaded.notes), Charsets.UTF_8)
            if (target.exists() && !target.delete()) error("Cannot replace previous backup")
            if (!temp.renameTo(target)) error("Cannot finalize backup")
            prefs.edit()
                .putLong("automatic_backup_last_run", System.currentTimeMillis())
                .putString("automatic_backup_last_status", "Success")
                .putInt("automatic_backup_last_count", loaded.notes.size)
                .apply()
            Result.success()
        } catch (_: Exception) {
            prefs.edit().putLong("automatic_backup_last_run", System.currentTimeMillis())
                .putString("automatic_backup_last_status", "Failed").apply()
            Result.retry()
        }
    }
}
