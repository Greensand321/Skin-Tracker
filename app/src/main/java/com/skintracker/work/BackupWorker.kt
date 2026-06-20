package com.skintracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skintracker.data.db.KetoDatabase
import com.skintracker.data.io.DataPortability
import com.skintracker.data.repository.DayRepository
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Periodic JSON backup worker — writes a timestamped backup file to
 * `getExternalFilesDir("backups")` (or internal storage as fallback) and
 * prunes the directory to the last 7 files. Instantiates its own
 * DayRepository directly from the singleton KetoDatabase so it can run
 * outside the ViewModel lifecycle.
 */
class BackupWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = DayRepository(KetoDatabase.get(applicationContext).dayEntryDao())
            val entries = repo.loadAll().associateBy { it.date }
            val json = DataPortability.encode(entries)

            val dir = applicationContext.getExternalFilesDir("backups")
                ?: File(applicationContext.filesDir, "backups").also { it.mkdirs() }
            dir.mkdirs()

            val ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
            File(dir, "keto_backup_$ts.json").writeText(json)

            // Prune to the 7 most recent backup files
            dir.listFiles { f -> f.isFile && f.name.startsWith("keto_backup_") && f.extension == "json" }
                ?.sortedByDescending { it.name }
                ?.drop(7)
                ?.forEach { it.delete() }

            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "keto_periodic_backup"

        fun schedule(context: Context, frequencyDays: Long) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<BackupWorker>(frequencyDays, TimeUnit.DAYS).build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
