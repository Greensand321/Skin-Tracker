package com.skintracker.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.skintracker.data.DateUtils
import com.skintracker.data.Meal
import com.skintracker.data.db.KetoDatabase
import com.skintracker.data.notifications.NotificationHelper
import com.skintracker.data.repository.DayRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Fires once per day at approximately the user-chosen hour. Reads today's entry
 * to build a context-aware message — if all three meals are already logged, the
 * notification is suppressed so the user is never nagged about something they
 * already did.
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val repo = DayRepository(KetoDatabase.get(applicationContext).dayEntryDao())
            val entry = repo.load(DateUtils.todayKey())

            val breakfastDone = !entry.mealEmpty(Meal.BREAKFAST)
            val lunchDone    = !entry.mealEmpty(Meal.LUNCH)
            val dinnerDone   = !entry.mealEmpty(Meal.DINNER)

            // All three meals logged — user is already on top of it. Skip the notification
            // so we never congratulate-nag someone who is already done for the day.
            if (breakfastDone && lunchDone && dinnerDone) return Result.success()

            val body = when {
                !breakfastDone && !lunchDone && !dinnerDone ->
                    "Open Skin Tracker to log today's meals and keep your streak going 💪"
                !dinnerDone ->
                    "Almost there — just log tonight's dinner to wrap up the day 🍽️"
                !lunchDone ->
                    "Don't forget lunch! A quick entry keeps your log complete 🥗"
                else ->
                    "A quick log is all it takes to keep your streak alive 🍳"
            }

            NotificationHelper.showReminder(applicationContext, body)
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "keto_daily_reminder"

        /**
         * Schedules (or reschedules) the daily reminder to fire at [hour]:00 local time.
         * WorkManager's minimum period is 15 minutes — the initial delay is calculated to
         * the next occurrence of [hour]:00 so the first fire is as close as possible, and
         * subsequent fires stay within a ~30-minute window of the target time.
         */
        fun schedule(context: Context, hour: Int) {
            val now = LocalDateTime.now()
            var target = now.toLocalDate().atTime(hour, 0)
            if (!target.isAfter(now)) target = target.plusDays(1)
            val delaySeconds = Duration.between(now, target).seconds

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                    .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                    .build(),
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
