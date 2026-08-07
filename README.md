# 📶 WiFi Captive Portal Tester PWA

[![Live PWA App](https://img.shields.io/badge/PWA-Live%20App-3b82f6?style=for-the-badge&logo=pwa)](https://abhijeetarjeet.github.io/wifi-portal-tester/)
[![Security](https://img.shields.io/badge/Privacy-100%25%20Local-10b981?style=for-the-badge)](https://github.com/AbhijeetArjeet/wifi-portal-tester)
[![Deploy](https://img.shields.io/badge/Deployment-GitHub%20Pages-0f172a?style=for-the-badge&logo=github)](https://abhijeetarjeet.github.io/wifi-portal-tester/)

A modern, mobile-first Progressive Web App (PWA) built for testing and validating Wi-Fi captive portal authentication systems safely with local CSV testing, **One-Tap Quick Connect**, **Auto-Login on Launch**, and **Failsafe Keep-Alive Auto-Relogin**.

🌐 **Live Application URL**: [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/)

---

## 🌟 Key Features

- **⭐ Personal Quick-Connect & Local Storage**: Save your personal credentials locally in your device's browser `localStorage` for instant one-tap logins.
- **⚡ Auto-Login on PWA Launch**: Opens the app and automatically authenticates your saved account immediately without requiring extra taps.
- **🛡️ Failsafe Keep-Alive (Session Timeout Prevention)**: Captive portals often disconnect users after 1–2 hours. The active background monitor periodically re-transmits the login payload (e.g. every 15–30 mins) and automatically re-connects whenever your device joins Wi-Fi or returns to the foreground.
- **📱 Mobile-First Responsive Design**: Optimized for Android phones, iPhones, iPads, and Windows/macOS laptops with native utility feel.
- **⚡ Offline App Shell (PWA)**: Installable directly to home screens with service worker offline caching and standalone display mode.
- **🔒 100% Client-Side Privacy**: Bulk CSV datasets remain strictly in volatile RAM. Personal credentials remain isolated in your local browser `localStorage`. Zero server uploads.
- **Flexible Header Auto-Mapping**: Parser automatically maps username and password columns from loaded CSV files.
- **🎲 Random & Controlled Testing**: Pick random credentials with masked password display (`**********`) and optional rate-limited batch testing.
- **📊 Real-Time Session Dashboard**: Tracks attempted tests, verified successes/failures, unverified submissions, and average response times.

---

## 📲 PWA Installation Guide

### 🤖 Android (Google Chrome / Edge / Brave)
1. Visit [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/).
2. Tap the **"Install App"** button on the header banner, or open the browser menu `⋮` at top-right.
3. Tap **"Add to Home Screen"** or **"Install app"**.
4. Launch the standalone app directly from your mobile home screen.

### 🍎 iPhone / iPad (iOS Safari)
1. Open [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/) in **Safari**.
2. Tap the **Share button** (square with up-arrow) on the bottom navigation bar.
3. Scroll down and tap **"Add to Home Screen"**.
4. Tap **Add** in the top-right corner.

---

## ⚡ How to Use Personal Quick-Connect & Failsafe

1. Open the app on your phone.
2. Enter your `Username` and `Password` under **Personal Quick-Connect & Failsafe**.
3. Tap **`💾 Save Credentials`**.
4. Check **`Auto-login automatically when PWA opens`** and **`Enable Failsafe Keep-Alive`**.
5. Every time you open the PWA from your home screen or get disconnected by the Wi-Fi portal timeout, the app automatically re-authenticates your device!

---

## 📂 CSV Dataset Format Example

Select a local `.csv` file containing test accounts:

```csv
Username,Password
user_alpha_101,pass_secret_991
user_beta_102,pass_secret_992
user_gamma_103,pass_secret_993
```

Once loaded, the UI will report:
`Accounts loaded: XXXX`

---

## 📡 Captive Portal Endpoint Payload

The POST request is transmitted to:
`POST https://captiveportal.kluniversity.in:8090/login.xml`

### Form Fields Submitted:
| Field Name | Value / Description |
| :--- | :--- |
| `mode` | `191` |
| `username` | Selected `Username` |
| `password` | Selected `Password` |
| `a` | Current timestamp string (`Date.now()`) |
| `producttype` | `0` |

---

## 📄 License
Internal Testing Tool • WiFi Captive Portal Tester
