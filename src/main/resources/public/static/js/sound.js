/*
 * Drink sound playback for both front ends, via Howler (loaded from the
 * howler webjar in fragments/sounds.html). Each entry in window.__SOUND_URLS__
 * is a [oggUrl, mp3Url] pair; Howler tries them in order and plays whichever
 * format the browser supports.
 */
(function (window) {
    'use strict';

    if (!window.Howl) {
        window.RyyppySound = {
            play: function () {},
            ready: window.Promise ? window.Promise.resolve() : null
        };
        return;
    }

    var clips = (window.__SOUND_URLS__ || []).map(function (src) {
        return new window.Howl({ src: src });
    });

    var ready = Promise.all(clips.map(function (clip) {
        return new Promise(function (resolve) {
            clip.once('load', resolve);
            clip.once('loaderror', resolve);
        });
    }));

    window.RyyppySound = {
        play: function () {
            if (!clips.length) {
                return;
            }
            clips[Math.floor(Math.random() * clips.length)].play();
        },
        ready: ready
    };
})(window);
