/*
 * Registers sw.js and reloads the tab as soon as it reports a new deploy,
 * with no prompt or confirmation.
 */
(function (window, navigator) {
    'use strict';

    if (!('serviceWorker' in navigator)) {
        return;
    }

    navigator.serviceWorker.addEventListener('message', function (event) {
        if (event.data && event.data.type === 'RYYPPY_NEW_VERSION') {
            console.log('New version detected, reloading.', event.data.detail);
            window.location.reload();
        }
    });

    // Deferred so registering the worker never competes with this page's
    // own resource fetches - see sound.js's preload for the same pattern.
    window.addEventListener('load', function () {
        navigator.serviceWorker.register('/sw.js');

        var meta = document.querySelector('meta[name="ryyppy-net-commit"]');
        var version = meta && meta.getAttribute('content');
        if (version) {
            navigator.serviceWorker.ready.then(function (registration) {
                registration.active.postMessage({ type: 'RYYPPY_PAGE_VERSION', version: version });
            });
        }
    });
})(window, navigator);
