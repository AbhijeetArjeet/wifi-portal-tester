# Building the Android App (with native app-wide DNS override)

This project is now wrapped with [Capacitor](https://capacitorjs.com), and includes a
custom native plugin (`DnsOverride`) that lets the app force **all apps on the phone**
to resolve DNS through a chosen server (Cloudflare 1.1.1.1, Google 8.8.8.8, or OpenDNS)
— without root.

## How the DNS override actually works

This is *not* a fake toggle — it's the same technique used by no-root DNS-changer apps
like DNS66 and Cloudflare's own 1.1.1.1 app:

1. `DnsVpnService` (`android/app/src/main/java/com/wifiportaltester/app/DnsVpnService.kt`)
   starts an Android `VpnService`.
2. Instead of routing **all** traffic through the tunnel (which would slow everything
   down and require a full user-space TCP/IP stack), it uses a **split-tunnel** trick:
   only a single fake address (`10.111.222.1`) is added as a route, and told to Android
   as the system DNS server. So **only DNS queries** get sent into our tunnel — every
   other byte of traffic (web browsing, video, etc.) goes over the normal network,
   completely unaffected.
3. The service reads the raw DNS query packets, forwards them to the real upstream
   DNS server (e.g. `1.1.1.1:53`) over a socket that's explicitly `protect()`-ed so it
   doesn't loop back into the VPN, and writes the real reply back into the tunnel with
   a correctly rebuilt IPv4/UDP header and checksum.
4. `DnsOverridePlugin.kt` is the Capacitor bridge: it handles the one-time Android
   "Connection request" VPN permission dialog, then starts/stops the service. It's
   exposed to the web app as `window.Capacitor.Plugins.DnsOverride` with `start()`,
   `stop()`, and `status()`.
5. In `index.html`, the DNS card now shows a native-only "App-wide DNS Override" panel
   (`initNativeDnsPanel()`) whenever it detects it's running inside the native shell
   (`Capacitor.isNativePlatform()`). In a regular browser/PWA it still shows the old
   "PWA DNS Limitation" notice — nothing about the web version changes.

**Expected behavior while active:** Android will show the small key/VPN icon in the
status bar and a persistent "Custom DNS active" notification. This is required by
Android for any VpnService-based app and can't be hidden — it's what tells the user
their traffic is being intercepted.

**Limitations:**
- UDP DNS (port 53) only — the vast majority of real-world DNS traffic. Apps that
  hard-code DNS-over-HTTPS/TLS to their own resolver (e.g. some browsers with DoH
  forced on) will bypass this, same as with 1.1.1.1/DNS66.
- Only one VPN can be active at a time — enabling this will disconnect any other
  active VPN app, and vice versa.
- Requires Android 7.0+ (API 24), which matches this project's existing `minSdkVersion`.

## Prerequisites

- [Android Studio](https://developer.android.com/studio) (includes the Android SDK)
- Node.js 18+ (already used by this project)

## Build steps

```bash
# 1. Install JS dependencies (Capacitor CLI etc.) — already done if you got this zip from Claude
npm install

# 2. Copy the web app into www/ and sync the Android project
npm run android:sync

# 3a. Open in Android Studio (recommended — handles SDK/signing/emulator for you)
npm run android:open

# 3b. OR build a debug APK from the command line
npm run android:build
# Output: android/app/build/outputs/apk/debug/app-debug.apk
```

Install on a device/emulator with the DNS panel enabled, tap **☁️ Use 1.1.1.1**,
approve the one-time "Connection request" VPN dialog, and every app on the phone
will now resolve DNS through Cloudflare.

## Whenever you change index.html / other web files

Re-run `npm run android:sync` before rebuilding — Capacitor copies your web assets
into `android/app/src/main/assets/public` and this step must happen before every
native build so the app shows your latest changes.

## Files added/changed for this feature

- `android/app/src/main/java/com/wifiportaltester/app/DnsVpnService.kt` — the VPN service
- `android/app/src/main/java/com/wifiportaltester/app/DnsOverridePlugin.kt` — Capacitor bridge plugin
- `android/app/src/main/java/com/wifiportaltester/app/MainActivity.java` — registers the plugin
- `android/app/src/main/AndroidManifest.xml` — VPN service declaration + permissions
- `android/app/build.gradle`, `android/build.gradle` — added Kotlin support
- `index.html` — native DNS override panel + JS glue (`initNativeDnsPanel`)
- `capacitor.config.json` — fixed `webDir` to `www`
- `package.json` — added `android:sync` / `android:open` / `android:build` scripts

---

## Persistent activity history (native + PWA)

A new **📜 Activity History** card in `index.html` logs speed tests, DNS speed
checks, native DNS override start/stop events, and portal login attempts to
`localStorage` (`wifi_tester_history`, capped at 200 entries, newest first).
This is pure JS — it works identically in the browser PWA and inside the
native app, no native code required.

- `loadHistory()` / `addHistoryEntry(type, data)` / `renderHistory()` / `clearHistory()`
  in `index.html` manage the store.
- Filter tabs (All / Speed / DNS / VPN / Login) filter the rendered list.
- Every `addHistoryEntry()` call also triggers `syncWidgetSnapshot()` (see the
  widget section below), so the home screen widget and the in-app history
  stay in sync from one code path.

## Background portal-login notifications (native only)

When a captive-portal login is triggered **in the background** (via the
Service Worker's periodic sync waking the page, see `sw.js`'s
`portal-keep-alive` tag → `AUTO_LOGIN` message → `connectSavedAccount({ fromAutoReconnect: true })`),
the app now posts a real Android notification so you find out the result
without reopening the app. Manual, foreground taps do **not** trigger a
notification — the in-app banner already covers that case, so this avoids
notification spam.

- `android/app/src/main/java/com/wifiportaltester/app/PortalNotifierPlugin.kt`
  — new Capacitor plugin, `PortalNotifier.notify({ success, title, body })`.
  Posts to its own channel (`portal_login_channel`) and opens the app on tap.
- `index.html` — `PortalNotifier` bridge + `notifyPortalResult()`, called from
  `updateStatsAndUI()` only when `fromAutoReconnect` is true.
- Uses the existing `POST_NOTIFICATIONS` permission (Android 13+) already
  declared for the DNS override feature — no manifest permission changes
  needed.

## Home screen widget: speed + DNS status at a glance (native only)

A resizable home screen widget shows your last speed test result, ping, and
native DNS override status, and updates instantly (not just on the OS's
~30-minute widget refresh cycle) whenever the app records a new speed test,
DNS test, or DNS override change.

- `android/app/src/main/java/com/wifiportaltester/app/StatusWidgetProvider.kt`
  — the `AppWidgetProvider`. Reads/writes a `SharedPreferences` snapshot
  (`wifi_tester_widget`) since a RemoteViews widget can't read the WebView's
  `localStorage` directly.
- `android/app/src/main/java/com/wifiportaltester/app/StatusWidgetPlugin.kt`
  — Capacitor bridge, `StatusWidget.updateStatus({ speedMbps, pingMs, dnsLabel, updatedAt })`,
  called from `syncWidgetSnapshot()` in `index.html`.
- `android/app/src/main/res/layout/widget_status.xml` — widget layout.
- `android/app/src/main/res/xml/status_widget_info.xml` — widget metadata
  (250×100dp min, resizable, `home_screen` category).
- `android/app/src/main/res/drawable/widget_background.xml` — rounded dark
  card background matching the app's theme.
- `AndroidManifest.xml` — registers the `<receiver>` for
  `StatusWidgetProvider` with the `APPWIDGET_UPDATE` intent-filter.
- Tapping the widget opens the app (same as tapping the app icon).

**To add the widget:** long-press your home screen → Widgets → WiFi Portal
Tester → drag "WiFi Portal Tester" onto the home screen. It'll show `--` for
speed/ping until you run your first speed test in the app.

**After pulling these changes:** run `npm run android:sync` and rebuild —
these are native-only files, so nothing changes in the JS build step, but
Android Studio (or `./gradlew`) needs to pick up the new Kotlin/XML files.
