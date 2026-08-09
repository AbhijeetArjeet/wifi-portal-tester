package com.wifiportaltester.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.getcapacitor.JSObject
import com.getcapacitor.Plugin
import com.getcapacitor.annotation.CapacitorPlugin

/**
 * Native network listener that solves the "SSID change" problem.
 *
 * Browsers (PWA) only fire 'online' events when the global connectivity state
 * flips. If you switch from one WiFi network to another, the browser often
 * stays "online" the whole time, so no event fires.
 *
 * This native plugin uses ConnectivityManager.NetworkCallback which notifies
 * the app on EVERY network transition (Available, CapabilitiesChanged, Lost),
 * allowing us to trigger a fresh captive portal check immediately.
 */
@CapacitorPlugin(name = "NetworkMonitor")
class NetworkMonitorPlugin : Plugin() {

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    override fun load() {
        super.load()
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                notifyStatusChange("available")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                // This triggers when transitioning between cell/wifi or switching SSIDs
                notifyStatusChange("capabilities_changed")
            }

            override fun onLost(network: Network) {
                notifyStatusChange("lost")
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, networkCallback!!)
    }

    private fun notifyStatusChange(status: String) {
        val ret = JSObject()
        ret.put("status", status)
        ret.put("timestamp", System.currentTimeMillis())
        notifyListeners("networkStatusChange", ret)
    }

    override fun handleOnDestroy() {
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }
        super.handleOnDestroy()
    }
}
