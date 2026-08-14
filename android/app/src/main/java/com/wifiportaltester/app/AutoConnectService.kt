package com.wifiportaltester.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

/**
 * AutoConnectService
 *
 * The native equivalent of the desktop client_auth_agent: a persistent
 * background watcher that detects captive portals / WiFi network changes
 * and re-submits the saved login automatically, without needing the app's
 * UI to be open.
 *
 * How detection works (two independent triggers, either can fire a login):
 *   1. NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL — Android's own
 *      captive-portal flag, set by the OS the moment it detects a WiFi
 *      network requires a login page.
 *   2. WiFi SSID change — if ACCESS_FINE_LOCATION was granted, we compare
 *      the current SSID against the last-seen one and treat any change as
 *      "you switched networks, try logging in again", covering campus
 *      portals that don't always surface the captive-portal capability
 *      reliably on reconnect.
 *
 * A 30s cooldown (matching the JS app's own anti-ban cooldown) prevents
 * hammering the portal if capability/SSID callbacks fire in quick bursts.
 */
class AutoConnectService : android.app.Service() {

    companion object {
        private const val TAG = "AutoConnectService"
        const val CHANNEL_ID = "auto_connect_channel"
        const val NOTIF_ID = 42002
        const val ACTION_STOP = "com.wifiportaltester.app.STOP_AUTO_CONNECT"
        private const val COOLDOWN_MS = 3_000L

        @Volatile var isRunning: Boolean = false
            private set
        @Volatile var lastAttemptAt: Long = 0L
            private set
        @Volatile var lastStatus: String = "idle"
            private set
        @Volatile var lastSsid: String = ""
            private set
    }

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var lastLoginAttemptMs: Long = 0L
    private var periodicThread: Thread? = null

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopWatching()
            return START_NOT_STICKY
        }

        if (!CredentialStore.isEnabled(this) || CredentialStore.getUsername(this).isNullOrBlank()) {
            Log.w(TAG, "No saved credentials — refusing to start background auto-connect")
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification("Watching for WiFi changes…"))
        startWatching()
        return START_STICKY
    }

    private fun startWatching() {
        if (isRunning) return
        isRunning = true
        updateNotification("Watching for WiFi changes…")

        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        lastSsid = currentSsid()

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Immediate check on WiFi connection
                checkAndMaybeLogin("network available", force = true)
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                if (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL)) {
                    checkAndMaybeLogin("captive portal capability detected", force = true)
                } else {
                    checkSsidChange()
                }
            }

            override fun onLost(network: Network) {
                updateNotification("WiFi disconnected — waiting to reconnect…")
            }
        }
        networkCallback = callback
        connectivityManager?.registerNetworkCallback(request, callback)

        // Periodic background poll every 25s: ensures auto-connect runs reliably
        // even if OS network callbacks are missed or throttled.
        periodicThread = thread(start = true, name = "AutoConnectPeriodicWatcher") {
            while (isRunning) {
                try {
                    Thread.sleep(25_000L)
                    if (isRunning && isOnWifi() && lastStatus != "success") {
                        checkAndMaybeLogin("periodic background probe", force = false)
                    }
                } catch (e: InterruptedException) {
                    break
                } catch (e: Exception) {
                    Log.w(TAG, "Periodic watcher error: ${e.message}")
                }
            }
        }
    }

    private fun checkSsidChange() {
        val ssid = currentSsid()
        if (ssid.isNotBlank() && ssid != "<unknown ssid>" && ssid != lastSsid) {
            lastSsid = ssid
            checkAndMaybeLogin("WiFi switched to $ssid", force = true)
        }
    }

    private fun checkAndMaybeLogin(reason: String, force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (now - lastLoginAttemptMs < COOLDOWN_MS)) {
            Log.d(TAG, "Skipping login attempt ($reason) — within 3s buffer")
            return
        }
        if (CredentialStore.getSkipOnMobileData(this) && !isOnWifi()) {
            return
        }
        lastLoginAttemptMs = now
        lastAttemptAt = now
        updateNotification("Logging in… ($reason)")
        thread(start = true, name = "AutoConnectLogin") { performLoginWithRetry() }
    }

    private fun isOnWifi(): Boolean {
        val cm = connectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    private fun currentSsid(): String {
        return try {
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ssid = wifiManager.connectionInfo?.ssid ?: return ""
            ssid.trim('"')
        } catch (e: SecurityException) {
            "" // ACCESS_FINE_LOCATION not granted — capability-based detection still works.
        } catch (e: Exception) {
            ""
        }
    }

    private fun performLoginWithRetry() {
        var attempts = 0
        val maxAttempts = 3
        var success = false

        while (attempts < maxAttempts && !success && isRunning) {
            attempts++
            success = performLogin()
            if (!success && attempts < maxAttempts && isRunning) {
                try {
                    Thread.sleep(3000L)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    /**
     * Mirrors the web app's sendPortalPOST(): form-urlencoded POST to the
     * Cyberoam/Sophos-style captive portal endpoint with mode=191.
     */
    private fun performLogin(): Boolean {
        val portalUrl = CredentialStore.getPortalUrl(this)
        val username = CredentialStore.getUsername(this)
        val password = CredentialStore.getPassword(this)
        if (portalUrl.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            lastStatus = "missing_credentials"
            updateNotification("No saved credentials")
            return false
        }

        try {
            val ts = System.currentTimeMillis().toString()
            val body = listOf(
                "mode" to "191",
                "username" to username,
                "password" to password,
                "a" to ts,
                "producttype" to "0"
            ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }

            val url = URL(portalUrl)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            val responseText = try {
                conn.inputStream.bufferedReader().readText()
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.readText() ?: ""
            }
            conn.disconnect()

            val ok = responseCode in 200..299 &&
                (responseText.contains("ack") || responseText.contains("message") ||
                    responseText.contains("success") || responseText.contains("live"))

            if (ok) {
                lastStatus = "success"
                updateNotification("Login successful — connected")
                notifyResult(true, "Connected to campus WiFi")
                return true
            } else {
                lastStatus = "failed_http_$responseCode"
                updateNotification("Login failed (HTTP $responseCode)")
                return false
            }
        } catch (e: Exception) {
            Log.w(TAG, "Login attempt failed: ${e.message}")
            lastStatus = "network_error"
            updateNotification("Portal unreachable — retrying shortly…")
            return false
        }
    }

    private fun buildNotification(text: String): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Background Auto-Connect",
                    NotificationManager.IMPORTANCE_LOW
                )
                channel.description = "Shows when background WiFi auto-login is active"
                nm.createNotificationChannel(channel)
            }
        }

        val stopIntent = Intent(this, AutoConnectService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return Notification.Builder(this).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
            setContentTitle("Auto-Connect running")
            setContentText(text)
            setSmallIcon(android.R.drawable.stat_notify_sync)
            setOngoing(true)
            addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
        }.build()
    }

    private fun updateNotification(text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification(text))
    }

    private fun notifyResult(success: Boolean, text: String) {
        // Small one-off heads-up alongside the persistent status notification,
        // so the user gets a signal even if they're not looking at the phone.
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val n = Notification.Builder(this).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) setChannelId(CHANNEL_ID)
            setContentTitle(if (success) "✅ Connected" else "⚠️ Connection issue")
            setContentText(text)
            setSmallIcon(android.R.drawable.stat_notify_sync)
            setAutoCancel(true)
        }.build()
        nm.notify(NOTIF_ID + 1, n)
    }

    private fun stopWatching() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (e: Exception) {
            Log.w(TAG, "unregisterNetworkCallback failed: ${e.message}")
        }
        networkCallback = null
        periodicThread?.interrupt()
        periodicThread = null
        isRunning = false
        lastStatus = "stopped"
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        stopWatching()
        super.onDestroy()
    }
}
