/*
 * Registers sw.js and reloads the tab the moment it reports a new deploy.
 * No prompt, no confirmation - see issue #151 for why that's deferred.
 */
(function (window, navigator) {
    'use strict';

    if (!('serviceWorker' in navigator)) {
        return;
    }

    navigator.serviceWorker.register('/sw.js');

    navigator.serviceWorker.addEventListener('message', function (event) {
        if (event.data && event.data.type === 'RYYPPY_NEW_VERSION') {
            window.location.reload();
        }
    });
})(window, navigator);
