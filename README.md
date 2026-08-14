# 📶 WiFi Captive Portal Tester PWA (v3.0 Campus Edition)

[![Live PWA App](https://img.shields.io/badge/PWA-v3.0%20Live%20App-3b82f6?style=for-the-badge&logo=pwa)](https://abhijeetarjeet.github.io/wifi-portal-tester/)
[![Security](https://img.shields.io/badge/Privacy-100%25%20Local-10b981?style=for-the-badge)](https://github.com/AbhijeetArjeet/wifi-portal-tester)
[![Deployment](https://img.shields.io/badge/Vercel-Deployment-000000?style=for-the-badge&logo=vercel)](https://wifi-portal-tester.vercel.app)

A modern, universal mobile-first Progressive Web App (PWA) built for student Wi-Fi auto-connection, campus captive portal validation, **Multi-Account Profile Switching**, **Auto-Detect Campus Gateways**, and a **Global Campus Wi-Fi Speed Leaderboard**.

🌐 **Live Application URL**: [https://abhijeetarjeet.github.io/wifi-portal-tester/](https://abhijeetarjeet.github.io/wifi-portal-tester/)  
⚡ **API Backend & Leaderboard**: Hosted on Vercel [https://wifi-portal-tester.vercel.app](https://wifi-portal-tester.vercel.app)

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

3. **Backend & Serverless Database APIs**:
   - **Vercel Serverless Edge Functions**: Lightweight serverless functions handle API endpoints (/api/submit-speed, /api/leaderboard).
   - **Upstash Redis (Serverless KV store)**: Ultra-low latency database deployed in the `ap-south-1` region (Mumbai) to record global speed test submissions and sync the university leaderboard instantly.

4. **Multi-CDN Speed Testing**:
   - **Cloudflare CDN / HTTP 3**: Leverages Cloudflare speed-measuring endpoints.
   - **Parallel Web Worker Streams**: Opens 8 concurrent TCP socket fetch connections to bypass single-thread latency limits, delivering precise bandwidth calculations matching speedtest.net accuracy.

---

## 🌟 Key Features

- **🏛️ Universal College Presets**: Pre-configured support for **KL University**, **SRM IST**, **VIT Vellore / Chennai**, **Manipal**, **Amity**, and others.
- **🔍 Auto-Detect Campus Gateway**: 1-click automatic captive portal URL scanning.
- **👥 Multi-Account Profile Switcher**: Save up to 3 student credentials with instant 1-tap swapping.
- **🚀 Campus Wi-Fi Speed Test & Global Leaderboard**: Real-time parallel download speed testing with public global ranking submissions.
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
⚡ Serverless Backend: Hosted on Vercel Serverless Functions (Node.js) for clean API scaling.
🗄️ Database: Upstash Redis (Serverless KV store) located in Mumbai (lowest latency) for instant leaderboard sync.
🚀 Bandwidth Engine: Uses parallel fetch streams (8 concurrent workers) hitting Cloudflare CDN endpoints to bypass single-thread latency limits for accurate speed results.

Key Features Built:
• Multi-Account Switcher (save up to 3 profiles for instant swapping)
• 1-Click captive portal Auto-Detection
• Failsafe Auto-Reconnect with Smart Network Check (skips login on mobile data automatically to save university server calls)
• Anti-Ban Protection (a 30s rate-limiting cooldown)
• Public Global Campus Wi-Fi Leaderboard (Vercel + Upstash Redis integration)

Check out the live code here: https://github.com/AbhijeetArjeet/wifi-portal-tester

#WebDevelopment #Javascript #PWA #Vercel #Serverless #Redis #Cloudflare #Productivity #PortfolioShowcase
```
