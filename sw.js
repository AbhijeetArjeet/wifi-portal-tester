const CACHE_NAME = "wifi-portal-tester-v7";
const ASSETS_TO_CACHE = [
  "./",
  "./index.html",
  "./manifest.webmanifest",
  "./icon.svg",
  "./icon-192.png",
  "./icon-512.png",
  "./apple-touch-icon.png"
];

// Install Event - Pre-cache all essential app shell files
self.addEventListener("install", (event) => {
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => cache.addAll(ASSETS_TO_CACHE))
  );
  self.skipWaiting();
});

// Activate Event - Clean up old cache versions immediately
self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.filter((key) => key !== CACHE_NAME).map((key) => caches.delete(key))
      )
    )
  );
  self.clients.claim();
});

// Fetch Event - Instant Offline Loading + Background Network Refresh
self.addEventListener("fetch", (event) => {
  if (event.request.method !== "GET" || event.request.url.includes("login.xml")) {
    return;
  }

  const isNavigation = event.request.mode === "navigate" || 
                       (event.request.headers.get("accept") && event.request.headers.get("accept").includes("text/html"));

  if (isNavigation) {
    event.respondWith(
      caches.match("./index.html").then((cachedResponse) => {
        const fetchPromise = fetch(event.request)
          .then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              const responseToCache = networkResponse.clone();
              caches.open(CACHE_NAME).then((cache) => {
                cache.put("./index.html", responseToCache);
                cache.put("./", responseToCache.clone());
              });
            }
            return networkResponse;
          })
          .catch(() => null);

        return cachedResponse || fetchPromise || caches.match("./");
      })
    );
    return;
  }

  // Static Assets (icons, manifest, etc.)
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        return cachedResponse;
      }
      return fetch(event.request).then((networkResponse) => {
        if (networkResponse && networkResponse.status === 200 && networkResponse.type === "basic") {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then((cache) => cache.put(event.request, responseToCache));
        }
        return networkResponse;
      });
    })
  );
});

// Background Sync & Periodic Background Sync Support
self.addEventListener("sync", (event) => {
  if (event.tag === "portal-auto-login") {
    event.waitUntil(notifyClientsToLogin());
  }
});

self.addEventListener("periodicsync", (event) => {
  if (event.tag === "portal-keep-alive") {
    event.waitUntil(notifyClientsToLogin());
  }
});

async function notifyClientsToLogin() {
  const allClients = await self.clients.matchAll({ includeUncontrolled: true, type: "window" });
  for (const client of allClients) {
    client.postMessage({ action: "AUTO_LOGIN" });
  }
}
