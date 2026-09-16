/*
 * Watches every fetch's X-App-Version header (AppVersionFilter) for drift,
 * then messages open tabs to reload - the worker has no DOM access to do
 * it itself. The baseline version lives in Cache Storage, not a plain
 * variable, since the worker can be terminated and restarted while idle.
 */
'use strict';

var VERSION_CACHE = 'ryyppy-sw-version';
var VERSION_KEY = new Request('/__sw_version_marker__');

var knownVersion = null;
var knownVersionLoaded = null;

self.addEventListener('install', function (event) {
    self.skipWaiting();
});

self.addEventListener('activate', function (event) {
    event.waitUntil(self.clients.claim());
});

function loadKnownVersion() {
    if (!knownVersionLoaded) {
        knownVersionLoaded = caches.open(VERSION_CACHE).then(function (cache) {
            return cache.match(VERSION_KEY);
        }).then(function (match) {
            return match ? match.text() : null;
        }).then(function (version) {
            knownVersion = version;
        });
    }
    return knownVersionLoaded;
}

function persistVersion(version) {
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

    return loadKnownVersion().then(function () {
        if (knownVersion === null) {
            knownVersion = version;
            return persistVersion(version);
        }
        if (knownVersion !== version) {
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
