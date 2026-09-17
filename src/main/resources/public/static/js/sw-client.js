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
            window.location.reload();
        }
    });

    // Deferred so registering the worker never competes with this page's
    // own resource fetches - see sound.js's preload for the same pattern.
    window.addEventListener('load', function () {
        navigator.serviceWorker.register('/sw.js');
    });
})(window, navigator);
