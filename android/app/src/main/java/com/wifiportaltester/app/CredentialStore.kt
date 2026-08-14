package com.wifiportaltester.app

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted-at-rest storage for the portal URL / username / password the
 * background AutoConnectService needs, since it must be able to read them
 * even after the app's WebView process (and its localStorage) is gone.
 *
 * Uses AndroidX Security's EncryptedSharedPreferences (AES256-GCM value
 * encryption, backed by a Keystore-protected master key) rather than plain
 * SharedPreferences, since this is real login credentials, not just a UI
 * preference.
 */
object CredentialStore {
    private const val PREFS_NAME = "auto_connect_secure_prefs"
    private const val KEY_PORTAL_URL = "portal_url"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_SKIP_ON_MOBILE = "skip_on_mobile_data"
    private const val KEY_ENABLED = "enabled"
    private const val TAG = "CredentialStore"

    private fun prefs(context: Context) = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Extremely unlikely (Keystore failure), but fall back to a
        // regular, unencrypted prefs file rather than crashing the service.
        Log.e(TAG, "EncryptedSharedPreferences unavailable, falling back to plain prefs", e)
        context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE)
    }

    fun save(context: Context, portalUrl: String, username: String, password: String, skipOnMobileData: Boolean) {
        prefs(context).edit()
            .putString(KEY_PORTAL_URL, portalUrl)
            .putString(KEY_USERNAME, username)
            .putString(KEY_PASSWORD, password)
            .putBoolean(KEY_SKIP_ON_MOBILE, skipOnMobileData)
            .putBoolean(KEY_ENABLED, true)
            .apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun getPortalUrl(context: Context): String? = prefs(context).getString(KEY_PORTAL_URL, null)
    fun getUsername(context: Context): String? = prefs(context).getString(KEY_USERNAME, null)
    fun getPassword(context: Context): String? = prefs(context).getString(KEY_PASSWORD, null)
    fun getSkipOnMobileData(context: Context): Boolean = prefs(context).getBoolean(KEY_SKIP_ON_MOBILE, true)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
