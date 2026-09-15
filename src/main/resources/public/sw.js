/*
 * Detects a new deploy landing under an already-open tab and reloads it.
 *
 * Every response carries an X-App-Version header (AppVersionFilter). This
 * worker doesn't cache anything - it passes every fetch through untouched
 * and only observes that header as a side effect, comparing it against the
 * first version it saw. A plain variable wouldn't survive the worker being
 * terminated and restarted while idle, so the baseline lives in Cache
 * Storage instead. On a mismatch it can't reload the page itself (no DOM
 * access), so it messages every controlled tab and lets sw-client.js do it.
 *
 * Static and stays that way: detection rides on the header, not on the
 * browser's own byte-diff update check for this file.
 */
'use strict';

var VERSION_CACHE = 'ryyppy-sw-version';
var VERSION_KEY = new Request('/__sw_version_marker__');

self.addEventListener('install', function (event) {
    self.skipWaiting();
});

self.addEventListener('activate', function (event) {
    event.waitUntil(self.clients.claim());
});

function getStoredVersion() {
    return caches.open(VERSION_CACHE).then(function (cache) {
        return cache.match(VERSION_KEY);
    }).then(function (match) {
        return match ? match.text() : null;
    });
}

function setStoredVersion(version) {
    return caches.open(VERSION_CACHE).then(function (cache) {
        return cache.put(VERSION_KEY, new Response(version));
    });
}

function notifyClients() {
    return self.clients.matchAll({ type: 'window' }).then(function (clients) {
        clients.forEach(function (client) {
            client.postMessage({ type: 'RYYPPY_NEW_VERSION' });
        });
    });
}

function checkVersion(response) {
    var version = response.headers.get('X-App-Version');
    if (!version) {
        return Promise.resolve();
    }

    return getStoredVersion().then(function (stored) {
        if (stored === null) {
            return setStoredVersion(version);
        }
        if (stored !== version) {
            return notifyClients();
        }
    });
}

self.addEventListener('fetch', function (event) {
    event.respondWith(
        fetch(event.request).then(function (response) {
            event.waitUntil(checkVersion(response));
            return response;
        })
    );
});
