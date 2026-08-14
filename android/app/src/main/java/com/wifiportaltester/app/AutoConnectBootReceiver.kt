package com.wifiportaltester.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts AutoConnectService after a device reboot, but only if the user
 * had background auto-connect actively enabled before the restart —
 * checked via CredentialStore, not just "credentials exist".
 */
class AutoConnectBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        if (!CredentialStore.isEnabled(context)) return
        if (CredentialStore.getUsername(context).isNullOrBlank()) return

        val serviceIntent = Intent(context, AutoConnectService::class.java)
        context.startForegroundService(serviceIntent)
    }
}
