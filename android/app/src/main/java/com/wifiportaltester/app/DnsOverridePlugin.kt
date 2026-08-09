package com.wifiportaltester.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Exposes native, app-wide DNS override to the web layer via:
 *   DnsOverride.start({ host, label })
 *   DnsOverride.stop()
 *   DnsOverride.status()
 *
 * Uses Android's VpnService — the same no-root mechanism used by apps like
 * Cloudflare's 1.1.1.1 and DNS66 — to force ALL apps on the device to
 * resolve DNS through the chosen server, while leaving non-DNS traffic
 * untouched (split-tunnel, see DnsVpnService for details).
 */
@CapacitorPlugin(name = "DnsOverride")
class DnsOverridePlugin : Plugin() {

    private var pendingHost: String? = null
    private var pendingLabel: String? = null

    @PluginMethod
    fun start(call: PluginCall) {
        val host = call.getString("host")
        if (host.isNullOrBlank()) {
            call.reject("Missing 'host' (DNS server IP)")
            return
        }
        val label = call.getString("label") ?: host

        pendingHost = host
        pendingLabel = label

        val prepareIntent = VpnService.prepare(context)
        if (prepareIntent != null) {
            // Need user's one-time consent via the system VPN confirmation dialog.
            startActivityForResult(call, prepareIntent, "vpnPermissionCallback")
        } else {
            // Already have permission (or previously granted for this app).
            launchVpnService(host, label)
            val ret = JSObject()
            ret.put("running", true)
            ret.put("label", label)
            call.resolve(ret)
        }
    }

    @ActivityCallback
    private fun vpnPermissionCallback(call: PluginCall?, result: androidx.activity.result.ActivityResult) {
        if (call == null) return
        val host = pendingHost
        val label = pendingLabel
        if (result.resultCode == Activity.RESULT_OK && host != null) {
            launchVpnService(host, label ?: host)
            val ret = JSObject()
            ret.put("running", true)
            ret.put("label", label)
            call.resolve(ret)
        } else {
            call.reject("VPN permission denied by user")
        }
    }

    private fun launchVpnService(host: String, label: String) {
        val intent = Intent(context, DnsVpnService::class.java).apply {
            putExtra(DnsVpnService.EXTRA_DNS_HOST, host)
            putExtra(DnsVpnService.EXTRA_DNS_LABEL, label)
        }
        context.startService(intent)
    }

    @PluginMethod
    fun stop(call: PluginCall) {
        val intent = Intent(context, DnsVpnService::class.java).apply {
            action = DnsVpnService.ACTION_STOP
        }
        context.startService(intent)
        val ret = JSObject()
        ret.put("running", false)
        call.resolve(ret)
    }

    @PluginMethod
    fun status(call: PluginCall) {
        val ret = JSObject()
        ret.put("running", DnsVpnService.isRunning)
        ret.put("label", DnsVpnService.activeDnsLabel)
        call.resolve(ret)
    }
}
