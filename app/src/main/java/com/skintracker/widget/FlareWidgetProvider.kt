package com.skintracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.skintracker.MainActivity
import com.skintracker.R

/**
 * Home-screen widget for Workflow B — logging a sudden flare-up without
 * opening the app first. The widget is a single button that deep-links
 * straight into the app's existing flare-entry sheet (it launches
 * [MainActivity] with [ACTION_LOG_FLARE]); the app then records the flare via
 * the same `AppViewModel.addFlare` path the in-app button uses. Keeping the
 * widget a thin launcher — rather than re-implementing symptom input in
 * RemoteViews — means there is one flare-entry UI to maintain, not two.
 */
class FlareWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        appWidgetIds.forEach { id -> manager.updateAppWidget(id, buildViews(context)) }
    }

    private fun buildViews(context: Context): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_flare)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_LOG_FLARE
            // SINGLE_TOP reuses the running instance (→ onNewIntent) instead of
            // stacking a second copy of the app on top of itself.
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            REQUEST_LOG_FLARE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_flare_button, pending)
        return views
    }

    companion object {
        /** Intent action set by the widget to open the app's flare-entry sheet. */
        const val ACTION_LOG_FLARE = "com.skintracker.action.LOG_FLARE"
        private const val REQUEST_LOG_FLARE = 1
    }
}
