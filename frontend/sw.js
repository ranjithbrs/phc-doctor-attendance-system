const CACHE_NAME = 'phc-attendance-v1';
const ASSETS_TO_CACHE = [
  './',
  './index.html',
  './docDashboard.html',
  './ddhcDashboard.html',
  './attendanceHistory.html',
  './register.html',
  './style.css',
  './config.js',
  './manifest.json'
];

// Install Event - Pre-cache core app shell assets
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      console.log('[Service Worker] Pre-caching App Shell');
      return cache.addAll(ASSETS_TO_CACHE);
    }).then(() => self.skipWaiting())
  );
});

// Activate Event - Clean up stale cache versions
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(cacheNames => {
      return Promise.all(
        cacheNames.map(cache => {
          if (cache !== CACHE_NAME) {
            console.log('[Service Worker] Deleting old cache:', cache);
            return caches.delete(cache);
          }
        })
      );
    }).then(() => self.clients.claim())
  );
});

// Fetch Event - Network-first for API requests, Cache-first for static assets
self.addEventListener('fetch', event => {
  const req = event.request;
  const url = new URL(req.url);

  // Network-first policy for API endpoints
  if (url.pathname.includes('/attendance/') || url.pathname.includes('/auth/') || url.pathname.includes('/dashboard/')) {
    event.respondWith(
      fetch(req).catch(() => {
        return new Response(JSON.stringify({
          error: 'Offline mode active. Your request will be queued locally for sync.',
          offline: true
        }), {
          headers: { 'Content-Type': 'application/json' }
        });
      })
    );
    return;
  }

  // Cache-first policy for static assets (HTML, CSS, JS)
  event.respondWith(
    caches.match(req).then(cachedResponse => {
      if (cachedResponse) {
        return cachedResponse;
      }
      return fetch(req).then(networkResponse => {
        if (req.method === 'GET' && networkResponse.status === 200) {
          const responseToCache = networkResponse.clone();
          caches.open(CACHE_NAME).then(cache => {
            cache.put(req, responseToCache);
          });
        }
        return networkResponse;
      });
    }).catch(() => {
      if (req.mode === 'navigate') {
        return caches.match('./index.html');
      }
    })
  );
});
