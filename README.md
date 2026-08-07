# 📶 WiFi Captive Portal Tester PWA

[![Live PWA App](https://img.shields.io/badge/PWA-Live%20App-3b82f6?style=for-the-badge&logo=pwa)](https://abhijeetarjeet.github.io/wifi-portal-tester/)
[![Security](https://img.shields.io/badge/Privacy-100%25%20Local-10b981?style=for-the-badge)](https://github.com/AbhijeetArjeet/wifi-portal-tester)
[![Deploy](https://img.shields.io/badge/Deployment-GitHub%20Pages-0f172a?style=for-the-badge&logo=github)](https://abhijeetarjeet.github.io/wifi-portal-tester/)

A modern, mobile-first Progressive Web App (PWA) built for testing and validating Wi-Fi captive portal authentication systems (`https://captiveportal.kluniversity.in:8090/login.xml`) safely using locally loaded CSV test accounts.

🌐 **Live Application URL**: [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/)

---

## 🌟 Key Features

- **📱 Mobile-First Responsive Design**: Optimized for Android phones, iPhones, iPads, and Windows/macOS laptops with native utility feel.
- **⚡ Offline App Shell (PWA)**: Installable directly to home screens with service worker offline caching and standalone display mode.
- **🔒 100% Client-Side Privacy**: CSV credential datasets are processed strictly in volatile browser RAM. Zero storage in `localStorage`, `IndexedDB`, `cookies`, or any remote server.
- **Header Auto-Mapping**: Flexible parser automatically normalizes column names:
  - `Reg.No` / `Reg No` / `RegNo` → **Username**
  - `App.No` / `App No` / `AppNo` → **Password**
  - `S.No` → **Ignored**
- **🎲 Random & Controlled Testing**: Pick random credentials with masked password display (`**********`) and optional rate-limited batch testing.
- **📊 Real-Time Session Dashboard**: Tracks attempted tests, verified successes/failures, unverified submissions, and average response times.
- **🌐 Network & CORS Aware**: Intelligent handling for captive portal endpoints with honest status reporting.

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

## 📂 CSV Dataset Specifications

Create or select a local `.csv` file containing test accounts:

```csv
S.No,App.No,Reg.No
1,APP1001,2500031388
2,APP1002,2500031389
3,APP1003,2500031390
```

### Supported Header Variations
- **Username column**: `Reg.No`, `Reg No`, `RegNo`, `Registered No`, `Registration No`, `username`
- **Password column**: `App.No`, `App No`, `AppNo`, `Application No`, `Application Number`, `password`
- **Ignored column**: `S.No` / `Serial No`

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
| `username` | Selected `Reg.No` |
| `password` | Selected `App.No` |
| `a` | Current timestamp string (`Date.now()`) |
| `producttype` | `0` |

---

## 🌐 Network & CORS Technical Analysis

Captive portal login endpoints operating on custom ports (e.g. `8090`) typically do not emit standard cross-origin headers (`Access-Control-Allow-Origin: *`).

1. **Execution**: The PWA submits requests via `fetch` with `mode: 'no-cors'` paired with an isolated hidden HTML form POST.
2. **Opaque Responses**: The browser executes the POST request over HTTPS but restricts reading the raw HTTP status and XML response body.
3. **Honest Reporting**: Instead of reporting false successes, the PWA session dashboard accurately logs:
   **`Submitted — response cannot be verified by browser`** alongside exact round-trip response timing (e.g. `145 ms`).
4. If network connection is severed or the captive portal is unreachable, the app labels it **`Network Error / Server Unreachable`**.

---

## 🛡️ Security Audit

- **No Secrets Committed**: `.gitignore` strictly blocks `*.csv`, `app_reg_numbers.csv`, `.env`, and credentials from git.
- **Zero Tracking**: No analytics, telemetry, or remote tracking scripts are embedded.

---

## 📄 License
Internal Testing Tool • WiFi Captive Portal Tester
