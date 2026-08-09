package com.wifiportaltester.app

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Home screen widget: shows the most recent speed test result, ping, and
 * native DNS override status "at a glance", without opening the app.
 *
 * Data flow: index.html calls the StatusWidget Capacitor plugin after every
 * speed test / DNS test / DNS override change (see syncWidgetSnapshot() in
 * index.html). The plugin writes the snapshot into SharedPreferences and
 * calls [pushUpdate], which immediately re-renders every placed widget.
 * [onUpdate] (the OS-driven periodic refresh, ~every 30 min) re-renders from
 * the same stored snapshot, so the widget is correct even if it's added
 * fresh or the process was killed since the last snapshot.
 */
class StatusWidgetProvider : AppWidgetProvider() {

    companion object {
        const val PREFS_NAME = "wifi_tester_widget"
        const val KEY_SPEED = "speedMbps"
        const val KEY_PING = "pingMs"
        const val KEY_DNS_LABEL = "dnsLabel"
        const val KEY_UPDATED_AT = "updatedAt"

        /** Called by StatusWidgetPlugin right after a fresh snapshot is saved. */
        fun pushUpdate(context: Context) {
            val mgr = AppWidgetManager.getInstance(context)
            val ids = mgr.getAppWidgetIds(ComponentName(context, StatusWidgetProvider::class.java))
            if (ids.isNotEmpty()) renderAll(context, mgr, ids)
        }

        private fun renderAll(context: Context, mgr: AppWidgetManager, ids: IntArray) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val speed = prefs.getFloat(KEY_SPEED, -1f)
            val ping = prefs.getInt(KEY_PING, -1)
            val dnsLabel = prefs.getString(KEY_DNS_LABEL, "") ?: ""
            val updatedAt = prefs.getLong(KEY_UPDATED_AT, 0L)

            val views = RemoteViews(context.packageName, R.layout.widget_status)

            views.setTextViewText(R.id.widgetSpeedValue, if (speed >= 0) String.format(Locale.US, "%.1f", speed) else "--")
            views.setTextViewText(R.id.widgetPingValue, if (ping >= 0) "Ping: ${ping} ms" else "Ping: -- ms")

            if (dnsLabel.isNotBlank()) {
                views.setTextViewText(R.id.widgetDnsStatus, "DNS: $dnsLabel")
            } else {
                views.setTextViewText(R.id.widgetDnsStatus, "DNS: Off")
            }

            views.setTextViewText(R.id.widgetUpdatedAt, if (updatedAt > 0) {
                "Updated: ${SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(updatedAt))}"
            } else {
                "Updated: never — open the app"
            })

            // Tapping the widget opens the app.
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launchIntent != null) {
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context, 0, launchIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                        (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) android.app.PendingIntent.FLAG_IMMUTABLE else 0)
                )
                views.setOnClickPendingIntent(R.id.widgetTitle, pendingIntent)
                views.setOnClickPendingIntent(R.id.widgetSpeedValue, pendingIntent)
            }

            for (id in ids) mgr.updateAppWidget(id, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        renderAll(context, appWidgetManager, appWidgetIds)
    }
}
