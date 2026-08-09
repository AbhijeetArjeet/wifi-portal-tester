// sw.js — WiFi Portal Tester v3.0 Service Worker
const CACHE_NAME = 'wifi-portal-tester-v11';
const ASSETS_TO_CACHE = [
  './',
  './index.html',
  './manifest.webmanifest',
  './icon.svg',
  './icon-192.png',
  './icon-512.png',
  './apple-touch-icon.png',
];

// ── Install: Pre-cache all app shell assets ──────────────────────────────────
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => cache.addAll(ASSETS_TO_CACHE))
  );
  self.skipWaiting();
});

// ── Activate: Remove old caches ──────────────────────────────────────────────
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k)))
    )
  );
  self.clients.claim();
});

// ── Fetch: Serve from cache; update in background ───────────────────────────
self.addEventListener('fetch', event => {
  const { request } = event;

  // Never intercept: login POSTs, API calls, or non-GET
  if (request.method !== 'GET') return;
  const url = request.url;
  if (url.includes('/api/') || url.includes('login.xml') || url.includes('fgtauth') ||
      url.includes('speed.cloudflare.com') || url.includes('connectivitycheck') ||
      url.includes('generate_204') || url.includes('dns-query') || url.includes('dns.google')) {
    return; // Let network handle these directly
  }

  const isNav = request.mode === 'navigate' ||
    (request.headers.get('accept') || '').includes('text/html');

  if (isNav) {
    event.respondWith(
      caches.match('./index.html').then(cached => {
        const net = fetch(request).then(res => {
          if (res && res.status === 200) {
            caches.open(CACHE_NAME).then(c => {
              c.put('./index.html', res.clone());
              c.put('./', res.clone());
            });
          }
          return res;
        }).catch(() => null);
        return cached || net || caches.match('./');
      })
    );
    return;
  }

  // Static assets — stale-while-revalidate
  event.respondWith(
    caches.match(request).then(cached => {
      const netFetch = fetch(request).then(res => {
        if (res && res.status === 200 && (res.type === 'basic' || res.type === 'cors')) {
          caches.open(CACHE_NAME).then(c => c.put(request, res.clone()));
        }
        return res;
      }).catch(() => null);
      return cached || netFetch;
    })
  );
});

// ── Background Sync: trigger login from SW ───────────────────────────────────
self.addEventListener('sync', event => {
  if (event.tag === 'portal-auto-login') {
    event.waitUntil(notifyClientsToLogin());
  }
});

self.addEventListener('periodicsync', event => {
  if (event.tag === 'portal-keep-alive') {
    event.waitUntil(notifyClientsToLogin());
  }
});

async function notifyClientsToLogin() {
  const clients = await self.clients.matchAll({ includeUncontrolled: true, type: 'window' });
  for (const client of clients) {
    client.postMessage({ action: 'AUTO_LOGIN', source: 'service-worker' });
  }
}

// ── Push Notifications (future) ───────────────────────────────────────────────
self.addEventListener('push', event => {
  const data = event.data ? event.data.json() : {};
  event.waitUntil(
    self.registration.showNotification(data.title || 'WiFi Portal Tester', {
      body: data.body || 'Tap to reconnect',
      icon: './icon-192.png',
      badge: './icon-192.png',
    })
  );
});

self.addEventListener('notificationclick', event => {
  event.notification.close();
  event.waitUntil(
    self.clients.matchAll({ type: 'window' }).then(clients => {
      if (clients.length) return clients[0].focus();
      return self.clients.openWindow('./');
    })
  );
});
