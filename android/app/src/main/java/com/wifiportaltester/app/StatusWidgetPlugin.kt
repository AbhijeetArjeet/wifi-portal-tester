package com.wifiportaltester.app

import android.content.Context
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Exposes the home screen widget's data source to the web layer via:
 *   StatusWidget.updateStatus({ speedMbps, pingMs, dnsLabel, updatedAt })
 *
 * The web app (index.html) calls this after every speed test, DNS test, or
 * DNS override change (see syncWidgetSnapshot()). We persist the snapshot in
 * SharedPreferences — the widget's only data source, since a RemoteViews
 * widget can't reach into the WebView's localStorage — and then immediately
 * repaint any placed widgets via StatusWidgetProvider.pushUpdate().
 */
@CapacitorPlugin(name = "StatusWidget")
class StatusWidgetPlugin : Plugin() {

    @PluginMethod
    fun updateStatus(call: PluginCall) {
        val speed = call.getDouble("speedMbps") ?: 0.0
        val ping = call.getInt("pingMs") ?: -1
        val dnsLabel = call.getString("dnsLabel") ?: ""
        val updatedAt = call.getLong("updatedAt") ?: System.currentTimeMillis()

        val prefs = context.getSharedPreferences(StatusWidgetProvider.PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat(StatusWidgetProvider.KEY_SPEED, speed.toFloat())
            .putInt(StatusWidgetProvider.KEY_PING, ping)
            .putString(StatusWidgetProvider.KEY_DNS_LABEL, dnsLabel)
            .putLong(StatusWidgetProvider.KEY_UPDATED_AT, updatedAt)
            .apply()

        StatusWidgetProvider.pushUpdate(context)

        val ret = JSObject()
        ret.put("saved", true)
        call.resolve(ret)
    }
}
