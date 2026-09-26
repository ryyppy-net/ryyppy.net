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

function notifyClients(detail, exceptClientId) {
    return self.clients.matchAll({ type: 'window' }).then(function (clients) {
        clients.forEach(function (client) {
            if (client.id !== exceptClientId) {
                client.postMessage({ type: 'RYYPPY_NEW_VERSION', detail: detail });
            }
        });
    });
}

// sourceClientId is a tab that already runs this version, so it is not told
// to reload.
function recordVersion(version, detail, sourceClientId) {
    return loadKnownVersion().then(function () {
        detail.previousVersion = knownVersion;
        detail.version = version;
        if (knownVersion === null) {
            console.log('[sw] Version baseline set', detail);
            knownVersion = version;
            return persistVersion(version);
        }
        if (knownVersion !== version) {
            console.log('[sw] Version changed', detail);
            knownVersion = version;
            return persistVersion(version).then(function () {
                return notifyClients(detail, sourceClientId);
            });
        }
    });
}

// Matches the paths AppVersionFilter stamps. Other responses can come from
// the CDN in front of production and carry an older deploy's version.
function isApiRequest(request) {
    var url = new URL(request.url);
    return url.origin === self.location.origin && /^\/api\//i.test(url.pathname);
}

function checkVersion(request, response) {
    var version = response.headers.get('X-App-Version');
    if (!version) {
        return Promise.resolve();
    }

    return recordVersion(version, {
        url: request.url,
        destination: request.destination,
        mode: request.mode,
        status: response.status
    });
}

// Sent by sw-client.js on page load with the commit rendered into the page.
self.addEventListener('message', function (event) {
    if (event.data && event.data.type === 'RYYPPY_PAGE_VERSION' && event.data.version) {
        var source = event.source || {};
        event.waitUntil(recordVersion(event.data.version, { url: source.url }, source.id));
    }
});

// Everything else is left to the browser, so the worker adds nothing to
// requests that can't carry a version.
self.addEventListener('fetch', function (event) {
    if (!isApiRequest(event.request)) {
        return;
    }
    event.respondWith(
        fetch(event.request).then(function (response) {
            event.waitUntil(checkVersion(event.request, response));
            return response;
        })
    );
});
