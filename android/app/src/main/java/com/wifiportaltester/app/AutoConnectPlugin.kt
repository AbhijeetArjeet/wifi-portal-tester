package com.wifiportaltester.app

import android.Manifest
import android.content.Intent
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback

/**
 * Exposes native background captive-portal auto-connect to the web layer:
 *
 *   AutoConnect.start({ portalUrl, username, password, skipOnMobileData })
 *   AutoConnect.stop()
 *   AutoConnect.status()
 *
 * Unlike the JS-side auto-reconnect logic (which only runs while the app's
 * WebView is open and foregrounded), this starts a real Android foreground
 * Service that keeps listening for WiFi network changes — including a
 * persistent captive-portal / SSID-change detector — even when the app is
 * closed, backgrounded, or the screen is off. Same category of feature as
 * the desktop client_auth_agent, but implemented the Android-native way.
 *
 * Reading the current WiFi SSID (used to detect "you switched networks")
 * requires ACCESS_FINE_LOCATION on Android 8 through 12, per platform
 * requirements — this does NOT track the device's physical location, it's
 * purely how Android gates WiFi scan/SSID info. If permission is denied,
 * the service still works using captive-portal capability detection alone,
 * just without the extra "SSID changed" trigger.
 */
@CapacitorPlugin(
    name = "AutoConnect",
    permissions = [
        Permission(strings = [Manifest.permission.ACCESS_FINE_LOCATION], alias = "location")
    ]
)
class AutoConnectPlugin : Plugin() {

    @PluginMethod
    fun start(call: PluginCall) {
        val portalUrl = call.getString("portalUrl")
        val username = call.getString("username")
        val password = call.getString("password")
        if (portalUrl.isNullOrBlank() || username.isNullOrBlank() || password.isNullOrBlank()) {
            call.reject("Missing 'portalUrl', 'username', or 'password'")
            return
        }
        val skipOnMobileData = call.getBoolean("skipOnMobileData", true) ?: true

        // Save credentials + settings so the background service can read them
        // even after the app process is killed and the service restarts.
        CredentialStore.save(context, portalUrl, username, password, skipOnMobileData)

        if (getPermissionState("location") != com.getcapacitor.PermissionState.GRANTED) {
            requestPermissionForAlias("location", call, "locationPermsCallback")
        } else {
            launchService(call)
        }
    }

    @PermissionCallback
    private fun locationPermsCallback(call: PluginCall) {
        // Proceed either way — SSID-change detection is a nice-to-have, not
        // required for captive-portal-capability-based detection to work.
        launchService(call)
    }

    private fun launchService(call: PluginCall) {
        val intent = Intent(context, AutoConnectService::class.java)
        context.startForegroundService(intent)
        val ret = JSObject()
        ret.put("running", true)
        call.resolve(ret)
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        val intent = Intent(context, AutoConnectService::class.java).apply {
            action = AutoConnectService.ACTION_STOP
        }
        context.startService(intent)
        CredentialStore.setEnabled(context, false)
        val ret = JSObject()
        ret.put("running", false)
        call.resolve(ret)
    }

    @PluginMethod
    fun status(call: PluginCall) {
        val ret = JSObject()
        ret.put("running", AutoConnectService.isRunning)
        ret.put("lastAttemptAt", AutoConnectService.lastAttemptAt)
        ret.put("lastStatus", AutoConnectService.lastStatus)
        ret.put("lastSsid", AutoConnectService.lastSsid)
        call.resolve(ret)
    }
}
