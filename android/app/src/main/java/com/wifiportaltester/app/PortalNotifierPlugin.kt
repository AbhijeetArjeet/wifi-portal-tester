package com.wifiportaltester.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import kotlin.random.Random

/**
 * Exposes a simple system notification to the web layer via:
 *   PortalNotifier.notify({ success, title, body })
 *
 * Used for background-triggered captive portal login attempts (e.g. from the
 * Service Worker's periodic sync / app-foregrounded checks) so the user finds
 * out the login succeeded or failed even if the app isn't on screen.
 *
 * This intentionally does NOT run its own background job — the web layer
 * (index.html / sw.js) still owns deciding *when* to check the portal. This
 * plugin only gives that JS-driven flow a real, persistent Android
 * notification instead of an in-page toast that nobody sees while the
 * screen is off or the app is backgrounded.
 */
@CapacitorPlugin(name = "PortalNotifier")
class PortalNotifierPlugin : Plugin() {

    companion object {
        const val CHANNEL_ID = "portal_login_channel"
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Portal Login Results",
                    NotificationManager.IMPORTANCE_DEFAULT
                )
                channel.description = "Notifies you when a background captive-portal login succeeds or fails"
                channel.enableLights(true)
                channel.lightColor = Color.BLUE
                nm.createNotificationChannel(channel)
            }
        }
    }

    @PluginMethod
    fun notify(call: PluginCall) {
        val success = call.getBoolean("success", false) ?: false
        val title = call.getString("title") ?: (if (success) "Connected" else "Login failed")
        val body = call.getString("body") ?: ""

        ensureChannel()

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val icon = if (success) android.R.drawable.stat_sys_download_done else android.R.drawable.stat_notify_error

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        // Only attach a tap action if we can actually resolve a launch intent —
        // PendingIntent.getActivity() with a null intent would crash.
        if (launchIntent != null) {
            val contentPending = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
            )
            builder.setContentIntent(contentPending)
        }

        val notification = builder.build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Unique-ish ID per call so consecutive results (e.g. a retry after a
        // failure) don't clobber each other before the user sees them.
        nm.notify(50000 + Random.nextInt(1000), notification)

        val ret = JSObject()
        ret.put("posted", true)
        call.resolve(ret)
    }
}
