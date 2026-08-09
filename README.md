# 📶 WiFi Portal Connector (v3.0 Campus Edition)

[![Live PWA App](https://img.shields.io/badge/PWA-v3.0%20Live%20App-3b82f6?style=for-the-badge&logo=pwa)](https://abhijeetarjeet.github.io/wifi-portal-tester/)
[![Security](https://img.shields.io/badge/Privacy-100%25%20Local-10b981?style=for-the-badge)](https://github.com/AbhijeetArjeet/wifi-portal-tester)
[![Deployment](https://img.shields.io/badge/Vercel-Deployment-000000?style=for-the-badge&logo=vercel)](https://wifi-portal-tester.vercel.app)

A modern, universal mobile-first app built for student Wi-Fi auto-connection, campus captive portal validation, **Multi-Account Profile Switching**, **Auto-Detect Campus Gateways**, and a **Global Campus Wi-Fi Speed Leaderboard**.

Now updated to **v3.0** with a full **Android Native App** implementation using Capacitor!

🌐 **Live Application URL**: [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/)  
⚡ **API Backend & Leaderboard**: Hosted on Vercel [https://wifi-portal-tester.vercel.app](https://wifi-portal-tester.vercel.app)

---

## 📱 Android Native App Features (New in v3.0)

While the PWA is great, the **Native Android App** (included in the `android/` folder) brings powerful hardware-level features that browsers cannot access:

1. **Native Network Monitor (Reliable Auto-Login)**:
   - Uses `ConnectivityManager.NetworkCallback` to listen for real hardware transitions.
   - Triggers portal checks immediately when switching WiFi networks or roaming between APs, even if the browser doesn't detect an "offline/online" flip.

2. **No-Root DNS Override (Split-Tunnel VPN)**:
   - Built-in `VpnService` implementation to resolve DNS through Cloudflare (1.1.1.1) or Google (8.8.8.8).
   - Solves the common "DNS-over-Campus-Wi-Fi" latency and blocking issues without needing root or a separate VPN app.

3. **Home Screen Status Widget**:
   - A native Android widget to see your last speed test, ping, and DNS status at a glance without opening the app.

4. **System Notifications**:
   - Persistent notifications for background portal login attempts so you know when you've been re-authenticated.

---

## 🛠️ The Tech Stack

1. **Frontend / UI**: Vanilla HTML5, Modern CSS3 (Glassmorphism), and ES6+ JavaScript.
2. **Native Layer**: **Capacitor 6** with custom Kotlin/Java Android plugins.
3. **Backend**: Vercel Serverless Edge Functions + Upstash Redis (Mumbai region).
4. **Bandwidth Engine**: Parallel TCP socket fetch workers for speedtest.net-level accuracy.

---

## 🌸 Easter Eggs (Oregairu Edition)
This version includes subtle thematic touches from the anime *My Teen Romantic Comedy SNAFU*:
- **The Genuine Connection**: Successful internet checks are now verified as "Genuine".
- **Service Club Monitor**: The auto-reconnect system is branded as the "Service Club's Network Monitor".
- **Hidden Quotes**: Tap the version footer text 5 times to see random quotes from Hachiman and the gang.

---

## 👔 LinkedIn Template (v3.0 Update)
```text
🚀 Major Update: WiFi Portal Connector v3.0 is now a Native Android App!

I've taken my "WiFi Portal Tester" PWA and evolved it into a full-featured Android application using Capacitor and custom Native Kotlin plugins.

What's new in the Native version?
📶 Native Network Callback: Solved the "SSID change" problem by using the Android ConnectivityManager to trigger auto-logins the second you switch WiFi networks.
🌐 System-wide DNS Override: Implemented a split-tunnel VpnService (no root required) to force DNS through Cloudflare 1.1.1.1, bypassing restrictive campus DNS.
📊 Home Screen Widget: Built a native widget to track real-time connection stats.
🌸 Oregairu Easter Eggs: Added some thematic flair from my favorite anime!

Check out the source code and download the latest APK: https://github.com/AbhijeetArjeet/wifi-portal-tester

#AndroidDevelopment #Kotlin #Capacitor #PWA #Vercel #Serverless #Productivity #GenuineConnect
```

---

## 👨‍💻 Developer
**Abhijeet Arjeet**  
[GitHub](https://github.com/AbhijeetArjeet/wifi-portal-tester) | [LinkedIn](https://www.linkedin.com/in/abhijeet-arjeet-021946251/)
