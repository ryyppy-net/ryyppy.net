/*
 * Drink sound playback.
 *
 * Both front ends share this one player: common.js's global playSound() for
 * the classic pages, and the AngularJS Sound service for the app.
 *
 * How it works:
 *
 *   1. The clip list comes from the server, already rendered into the page as
 *      window.__SOUND_URLS__ (SoundManifest -> fragments/sounds.html), so
 *      there is no manifest request and no path building here. The URLs are
 *      content-hashed, which is what lets /static/sounds/** be served
 *      immutable: fetched once per browser, then answered from disk cache
 *      forever, and a replaced clip gets a new URL on its own.
 *   2. Every clip is fetched and decoded into an AudioBuffer once, after the
 *      page has finished loading, and kept in memory for the life of the page.
 *      Playing is then wiring an AudioBufferSourceNode to the output and
 *      calling start() - no network, no decode, no seeking, so a click makes a
 *      sound in the same frame it happened.
 *   3. Because every play() gets a fresh source node, a clip can overlap
 *      itself. The old <audio>-element pool could not: replaying an element
 *      mid-playback restarted it.
 *
 * Only MP3 is listed in the manifest, and every browser decodes MP3, so there
 * is no format negotiation. Note that canPlayType() would be the wrong probe
 * here in any case - it describes <audio> element playback, which does not
 * always agree with what decodeAudioData() accepts.
 *
 * Requires an unprefixed AudioContext (Chrome 35, Firefox 25, Safari 14.1,
 * iOS Safari 14.5). That is also exactly when decodeAudioData() started
 * returning a promise, so both can be assumed together. Older browsers get a
 * silent no-op rather than a fallback path that nothing would exercise.
 *
 * Autoplay policies (notably iOS Safari's) only let audio start after a user
 * gesture, and playSound() usually runs later, from an API response - so the
 * AudioContext is resumed on the first real click/touch/key press.
 */
(function (window, document) {
    'use strict';

    if (!window.AudioContext || !window.fetch) {
        window.RyyppySound = { play: function () {} };
        return;
    }

    var urls = window.__SOUND_URLS__ || [];

    // Constructing the context before a gesture is allowed; it just starts out
    // suspended. Doing it now rather than on the first click means decoding
    // can happen while the user is still reading the page.
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
                // A clip that will not load or decode is simply never played.
            });
    }

    /*
     * One clip at a time, rather than all at once. The party pages poll the
     * API continuously while open, and the clips are ~490 KB in total; loading
     * them serially keeps that off the polling's back on a slow connection.
     * Nothing waits on them - the sound is wanted at the end of a 5s undo
     * countdown at the earliest.
     */
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
                return; // nothing decoded yet - stay silent rather than lag
            }

            resume();

            var source = context.createBufferSource();
            source.buffer = buffers[Math.floor(Math.random() * buffers.length)];
            source.connect(context.destination);
            source.start();
        }
    };
})(window, document);
