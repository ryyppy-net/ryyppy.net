/*
 * Drink sound playback for both front ends, via Howler (loaded from the
 * howler webjar in fragments/sounds.html). Each entry in window.__SOUND_URLS__
 * is a [oggUrl, mp3Url] pair; Howler tries them in order and plays whichever
 * format the browser supports.
 *
 * Some clips begin with silence, which reads as lag after the click.
 * LEAD_IN_SECONDS gives, per file stem, where the clip becomes audible
 * (measured at -50 dB, minus a 5 ms margin); playback starts there.
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

    var LEAD_IN_SECONDS = {
        '1': 0.062,
        '2': 0.103,
        '3': 0.138,
        '4': 0.212,
        '5': 0.084,
        '6': 0.212
    };

    function stemOf(url) {
        return url.split('/').pop().replace(/\.[^.]+$/, '');
    }

    var clips = (window.__SOUND_URLS__ || []).map(function (src) {
        return {
            howl: new window.Howl({ src: src }),
            offset: LEAD_IN_SECONDS[stemOf(src[0])] || 0
        };
    });

    var ready = Promise.all(clips.map(function (clip) {
        return new Promise(function (resolve) {
            clip.howl.once('load', resolve);
            clip.howl.once('loaderror', resolve);
        });
    }));

    window.RyyppySound = {
        play: function () {
            if (!clips.length) {
                return;
            }
            var clip = clips[Math.floor(Math.random() * clips.length)];
            var id = clip.howl.play();
            if (clip.offset > 0) {
                clip.howl.seek(clip.offset, id);
            }
        },
        ready: ready
    };
})(window);
