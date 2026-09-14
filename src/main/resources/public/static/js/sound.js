/*
 * Drink sound playback for both front ends.
 *
 * Clips are decoded once and played from memory, so a click starts a sound
 * with no fetch or decode. Autoplay policies keep an AudioContext suspended
 * until a user gesture, so the first input resumes it. The clip list is
 * rendered into the page as window.__SOUND_URLS__.
 */
(function (window, document) {
    'use strict';

    if (!window.AudioContext || !window.fetch) {
        window.RyyppySound = { play: function () {} };
        return;
    }

    var urls = window.__SOUND_URLS__ || [];

    // Allowed before a gesture; the context simply starts out suspended.
    var context = new window.AudioContext();
    var buffers = [];

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
        }, Promise.resolve());
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
        }
    };
})(window, document);
