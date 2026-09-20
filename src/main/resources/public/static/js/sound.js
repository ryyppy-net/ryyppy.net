/*
 * Drink sound playback for both front ends.
 *
 * Clips are decoded once and played from memory, so a click starts a sound
 * with no fetch or decode. Autoplay policies keep an AudioContext suspended
 * until a user gesture, so the first input resumes it. The clip list is
 * rendered into the page as window.__SOUND_URLS__.
 *
 * Loading is observable: state() is 'loading' until the first clip decodes,
 * then 'ready', or 'failed' once every clip has been tried and none decoded.
 * 'unavailable' means the browser has no Web Audio. ready resolves with that
 * settled state. A clip that never loads is otherwise indistinguishable from
 * one that is merely slow, since play() is silent either way.
 */
(function (window, document) {
    'use strict';

    if (!window.AudioContext || !window.fetch) {
        window.RyyppySound = {
            play: function () {},
            state: function () { return 'unavailable'; },
            decodedCount: function () { return 0; },
            clipCount: function () { return 0; },
            // A browser without fetch may predate Promise too.
            ready: window.Promise ? window.Promise.resolve('unavailable') : null
        };
        return;
    }

    var urls = window.__SOUND_URLS__ || [];

    // Allowed before a gesture; the context simply starts out suspended.
    var context = new window.AudioContext();
    var buffers = [];

    var state = 'loading';
    var settleReady;
    var ready = new Promise(function (resolve) {
        settleReady = resolve;
    });

    function settle(settledState) {
        if (state !== 'loading') {
            return;
        }
        state = settledState;
        settleReady(settledState);
    }

    function load(url) {
        return window.fetch(url, { credentials: 'same-origin' })
            .then(function (response) {
                if (!response.ok) {
                    throw new Error('HTTP ' + response.status);
                }
                return response.arrayBuffer();
            })
            .then(function (data) {
                return context.decodeAudioData(data);
            })
            .then(function (buffer) {
                buffers.push(buffer);
                settle('ready');
            })
            .catch(function () {
                // A clip that will not load or decode is never played.
            });
    }

    // One clip at a time: the party pages poll the API continuously while
    // open, and the clips are ~490 KB in total.
    function loadAll() {
        urls.reduce(function (previous, url) {
            return previous.then(function () {
                return load(url);
            });
        }, Promise.resolve()).then(function () {
            settle('failed');
        });
    }

    if (document.readyState === 'complete') {
        loadAll();
    } else {
        window.addEventListener('load', loadAll);
    }

    function resume() {
        if (context.state === 'suspended') {
            context.resume();
        }
    }

    ['pointerdown', 'touchend', 'keydown'].forEach(function (event) {
        document.addEventListener(event, function armOnce() {
            document.removeEventListener(event, armOnce, true);
            resume();
        }, true);
    });

    window.RyyppySound = {
        play: function () {
            if (!buffers.length) {
                return; // nothing decoded yet
            }

            resume();

            var source = context.createBufferSource();
            source.buffer = buffers[Math.floor(Math.random() * buffers.length)];
            source.connect(context.destination);
            source.start();
        },

        state: function () {
            return state;
        },

        decodedCount: function () {
            return buffers.length;
        },

        clipCount: function () {
            return urls.length;
        },

        ready: ready
    };
})(window, document);
