'use strict';

// Stale-while-revalidate for Gravatar avatar images. These are requested as
// <img> elements (no-cors mode, so the fetch() response is opaque - status 0,
// body unreadable), and Gravatar sends a validating Cache-Control that forces
// the browser to round-trip for a 304 on every load even though the image
// rarely changes. Serving straight from Cache Storage skips that round trip;
// the background fetch keeps the cached copy from going permanently stale.
const GRAVATAR_CACHE = 'gravatar-v1';

function isGravatarAvatarRequest(request) {
    return request.method === 'GET' && request.url.indexOf('//www.gravatar.com/avatar/') !== -1;
}

self.addEventListener('install', function (event) {
    self.skipWaiting();
});

self.addEventListener('activate', function (event) {
    event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', function (event) {
    if (!isGravatarAvatarRequest(event.request)) {
        return;
    }

    event.respondWith(
        caches.open(GRAVATAR_CACHE).then(function (cache) {
            return cache.match(event.request).then(function (cachedResponse) {
                const revalidate = fetch(event.request.clone(), {cache: 'reload'})
                    .then(function (networkResponse) {
                        // Opaque cross-origin responses report status 0 / ok === false,
                        // so we can't use response.ok to detect a real failure here.
                        if (networkResponse.type === 'opaque' || networkResponse.ok) {
                            cache.put(event.request, networkResponse.clone());
                        }
                        return networkResponse;
                    })
                    .catch(function () {
                        return cachedResponse;
                    });

                return cachedResponse || revalidate;
            });
        })
    );
});
