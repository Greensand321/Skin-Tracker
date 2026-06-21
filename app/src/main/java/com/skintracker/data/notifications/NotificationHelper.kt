package com.skintracker.data.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.skintracker.MainActivity
import com.skintracker.R

/** Handles the notification channel lifecycle and builds the daily reminder notification. */
object NotificationHelper {

    const val CHANNEL_ID = "skin_daily_reminder"
    private const val NOTIFICATION_ID = 1001

    /**
     * Creates (or updates) the notification channel — safe to call on every app start since
     * Android ignores duplicate channel creation calls once the channel already exists.
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Reminder",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily nudge to log today's skin symptoms"
            enableLights(true)
            lightColor = ContextCompat.getColor(context, R.color.skin_accent)
            enableVibration(true)
            setShowBadge(true)
        }
        notificationManager(context).createNotificationChannel(channel)
    }

    /**
     * Builds and posts the daily reminder.
     *
     * Visual design goals — good on both lock screen and notification bar:
     *  - Large icon: full-colour app launcher icon (shows in the notification body)
     *  - Small icon: monochrome skin cross-section silhouette (tinted amber in the status bar)
     *  - BigTextStyle: expanded body text when the shade is pulled down
     *  - VISIBILITY_PUBLIC: shows full content on the lock screen without requiring unlock
     *  - Amber accent colour: on-brand with the skin tracker palette
     *  - "Log Now" action button: one-tap to open the app directly
     */
    fun showReminder(context: Context, body: String) {
        val tapIntent = buildTapIntent(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle("Skin Tracker")
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
                    .setBigContentTitle("Skin Tracker")
                    .setSummaryText("Daily Reminder"),
            )
            // Amber accent: tints the small icon in the status bar and the left-hand stripe
            // on MIUI / Samsung One UI lock-screen cards.
            .setColor(ContextCompat.getColor(context, R.color.skin_accent))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            // Show full notification text on the lock screen — no need to unlock to read it.
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // One-tap action button shown both in the notification bar and on the lock screen.
            .addAction(
                R.drawable.ic_notification,
                "Log Now",
                tapIntent,
            )
            .build()

        notificationManager(context).notify(NOTIFICATION_ID, notification)
    }

    private fun buildTapIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
