# 📶 WiFi Captive Portal Tester PWA (v3.0 Campus Edition)

[![Live PWA App](https://img.shields.io/badge/PWA-v3.0%20Live%20App-3b82f6?style=for-the-badge&logo=pwa)](https://abhijeetarjeet.github.io/wifi-portal-tester/)
[![Security](https://img.shields.io/badge/Privacy-100%25%20Local-10b981?style=for-the-badge)](https://github.com/AbhijeetArjeet/wifi-portal-tester)
[![Deployment](https://img.shields.io/badge/Supabase-Backend-3ecf8e?style=for-the-badge&logo=supabase)](https://supabase.com)

A modern, universal mobile-first Progressive Web App (PWA) built for student Wi-Fi auto-connection, campus captive portal validation, **Multi-Account Profile Switching**, **Auto-Detect Campus Gateways**, and a **Global Campus Wi-Fi Speed Leaderboard**.

🌐 **Live Application URL**: [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/)  
⚡ **Leaderboard Backend**: Supabase (PostgREST) — no custom server required

---

## 🛠️ The Tech Stack (What We Are Using)

The project is designed to be lightweight, serverless, and extremely fast on poor connections:

1. **Frontend / UI**:
   - **HTML5 & Vanilla ES6+ JavaScript**: Lightweight, zero dependencies, no heavy framework overhead, rendering in milliseconds.
   - **CSS3 Modern Custom Styles**: Implements glassmorphism, responsive CSS grid layouts, smooth micro-animations, and styled HSL-tailored color palettes.
   - **SVG Path Gauges**: Interactive gauges dynamically rendered in real-time.

2. **Offline-First & PWA Capabilities**:
   - **Service Workers (`sw.js`)**: Background caching of assets for offline readiness, allowing immediate app launches even with zero internet signal.
   - **Web App Manifest (`manifest.webmanifest`)**: Enforces standalone PWA app shell, allowing installation on Android/iOS home screens.

3. **Backend & Database**:
   - **Supabase (PostgREST)**: The leaderboard reads and writes directly against Supabase's auto-generated REST API — no custom server required. Submissions fall back to local storage if offline.

4. **Native Android Shell (Capacitor)**:
   - **Capacitor + Kotlin**: Wraps the PWA in a native Android app for features browsers can't provide.
   - **VpnService-based DNS Override**: A no-root, split-tunnel DNS changer (same technique as Cloudflare's 1.1.1.1 app) — only DNS traffic is intercepted, browsing speed is untouched.
   - **Native notifications & home-screen widget**: System push notifications on login result, plus a widget showing the latest speed/DNS snapshot.

5. **Multi-CDN Speed Testing**:
   - **Cloudflare CDN / HTTP 3**: Leverages Cloudflare speed-measuring endpoints.
   - **Parallel Web Worker Streams**: Opens 8 concurrent TCP socket fetch connections to bypass single-thread latency limits, delivering precise bandwidth calculations matching speedtest.net accuracy.

---

## 🌟 Key Features

- **🔑 Captive Portal Auto-Login**: Detects the network state (open / captive / offline) and logs in automatically — on launch, on network change, on device wake, or when the app is foregrounded.
- **🏛️ Universal College Presets**: Pre-configured support for **KL University**, **SRM IST**, **VIT Vellore / Chennai**, **Manipal**, **Amity**, and others.
- **🔍 Auto-Detect Campus Gateway**: 1-click automatic captive portal URL scanning.
- **👥 Multi-Account Profile Switcher**: Save multiple student credential profiles with instant 1-tap swapping.
- **🚀 Real Wi-Fi Speed Test**: Opens 4–8 parallel connections against Cloudflare's speed CDN (mirroring how Ookla/Speedtest.net measure high-bandwidth links) to report download speed, ping, and jitter.
- **🌍 Global Speed Leaderboard**: Submit results and see how your campus ranks worldwide, backed live by Supabase — with automatic local fallback if you're offline.
- **🔒 Native DNS Override** *(Android app only)*: One-tap switch between Cloudflare 1.1.1.1, Google 8.8.8.8, and OpenDNS using a real no-root VPN service — browsing traffic is untouched, only DNS queries are routed through it. The PWA shows manual setup steps instead, since browsers can't do this natively.
- **📜 Activity History**: A persistent, filterable log of every speed test, DNS check, DNS override, and portal login — stored locally, survives app restarts.
- **🛡️ Smart Reconnect & Anti-Ban Cooldown**: Auto-detects captive portals while safely skipping login on mobile data, with a 30s throttling cooldown to prevent server/IP bans.
- **❌ Factory Data Reset**: 1-tap browser cache and saved accounts wipe.

---

## 📲 PWA Installation Guide

### 🤖 Android (Google Chrome / Edge / Brave)
1. Visit the app link.
2. Tap the **"Install"** button on the header banner, or open the browser menu `⋮`.
3. Tap **"Add to Home Screen"** or **"Install app"**.

### 🍎 iPhone / iPad (iOS Safari)
1. Open the URL in **Safari**.
2. Tap the **Share button** (square with up-arrow) on the bottom navigation bar.
3. Scroll down and tap **"Add to Home Screen"**.

---

## 👔 LinkedIn Showcase Post Template
*Feel free to copy, modify, and share this on your LinkedIn profile!* 🚀

```text
🚀 Project Showcase: Building a Serverless PWA for College Captive Portal Testing & Wi-Fi Analytics!

Tired of manually logging into your campus Wi-Fi captive portal every time your phone locks? I built a solution to solve this and add some fun university-wide features to the mix!

Introducing 📶 WiFi Portal Tester v3.0 — a Progressive Web App (PWA) with a serverless backend and a live campus speed test leaderboard.

Here is the tech stack behind it:
💻 Frontend: Vanilla HTML5, Modern CSS3 (Glassmorphism), and ES6+ JavaScript. No bulky frameworks, ensuring near-instant load times even on spotty college networks.
📲 Progressive Web App (PWA): Equipped with Service Workers and Cache Storage API so the app launches and runs offline.
🗄️ Database: Supabase (PostgREST) for instant, serverless leaderboard sync straight from the client.
🚀 Bandwidth Engine: Uses parallel fetch streams (4–8 concurrent workers) hitting Cloudflare CDN endpoints to bypass single-thread latency limits for accurate speed results.
📱 Native Android Shell: Capacitor + Kotlin, with a real no-root VpnService for app-wide DNS override.

Key Features Built:
• Multi-Account Switcher for instant profile swapping
• 1-Click captive portal Auto-Detection
• Failsafe Auto-Reconnect with Smart Network Check (skips login on mobile data automatically to save university server calls)
• Anti-Ban Protection (a 30s rate-limiting cooldown)
• Public Global Campus Wi-Fi Leaderboard (Supabase-backed)
• Native DNS Override on Android (Cloudflare / Google / OpenDNS, one tap)

Check out the live code here: https://github.com/AbhijeetArjeet/wifi-portal-tester

#WebDevelopment #Javascript #PWA #Supabase #Serverless #Cloudflare #Android #Kotlin #Productivity #PortfolioShowcase
```
