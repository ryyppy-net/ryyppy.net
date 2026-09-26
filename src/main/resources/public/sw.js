/*
 * Watches fetches for X-App-Version drift (AppVersionFilter) and messages
 * open tabs to reload, since a worker can't reload them itself. The
 * baseline lives in Cache Storage, since a worker can be killed while idle.
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

function notifyClients(detail) {
    return self.clients.matchAll({ type: 'window' }).then(function (clients) {
        clients.forEach(function (client) {
            client.postMessage({ type: 'RYYPPY_NEW_VERSION', detail: detail });
        });
    });
}

function describe(request, response, version) {
    return {
        previousVersion: knownVersion,
        version: version,
        url: request.url,
        destination: request.destination,
        mode: request.mode,
        status: response.status
    };
}

// Matches the paths AppVersionFilter stamps. Other responses can come from
// the CDN in front of production and carry an older deploy's version.
function isApiRequest(request) {
    var url = new URL(request.url);
    return url.origin === self.location.origin && /^\/api\//i.test(url.pathname);
}

function checkVersion(request, response) {
    var version = response.headers.get('X-App-Version');
    if (!version || !isApiRequest(request)) {
        return Promise.resolve();
    }

    return loadKnownVersion().then(function () {
        if (knownVersion === null) {
            console.log('[sw] Version baseline set', describe(request, response, version));
            knownVersion = version;
            return persistVersion(version);
        }
        if (knownVersion !== version) {
            var detail = describe(request, response, version);
            console.log('[sw] Version changed', detail);
            knownVersion = version;
            return persistVersion(version).then(function () {
                return notifyClients(detail);
            });
        }
    });
}

self.addEventListener('fetch', function (event) {
    event.respondWith(
        fetch(event.request).then(function (response) {
            event.waitUntil(checkVersion(event.request, response));
            return response;
        })
    );
});
