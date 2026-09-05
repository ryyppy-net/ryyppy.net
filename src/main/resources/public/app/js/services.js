/*global angular */

(function (angular) {
    'use strict';

    /**
     * RyyppyAPI is used to handle all API operations. It depends on Angular $http
     * service.
     */
    function RyyppyAPI(http) {
        this._baseUrl = "/API/v2";

        // AngularJS would prefer to use application/json but as our backend
        // doesn't support this yet, fallback type must be set.
        http.defaults.headers.post["Content-Type"] = "application/x-www-form-urlencoded";

        this.getProfile = function (callbackSuccess) {
            http.get(this._baseUrl + "/profile").success(callbackSuccess);
        };

        this.updateProfile = function (profile, callbackSuccess) {
            http.post(this._baseUrl + "/profile", $.param(profile)).success(callbackSuccess);
        };

        this.getOwnDrinks = function (callbackSuccess) {
            http.get(this._baseUrl + "/profile/drinks").success(callbackSuccess);
        };

        this.getParties = function (callbackSuccess) {
            http.get(this._baseUrl + "/parties").success(callbackSuccess);
        };

        this.getParty = function (partyId, callbackSuccess) {
            http.get(this._baseUrl + "/parties/" + partyId).success(callbackSuccess);
        };

        this.getPartyParticipants = function (partyId, callbackSuccess) {
            http.get(this._baseUrl + "/parties/" + partyId + "/participants").success(callbackSuccess);
        };

        this.getPartyInvitations = function (partyId, callbackSuccess) {
            http.get(this._baseUrl + "/parties/" + partyId + "/invitations").success(callbackSuccess);
        };

        this.inviteUser = function (userId, partyId, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/invitations";
            http.post(url, $.param({ userId : userId })).success(callbackSuccess);
        };

        this.addDrink = function (partyId, participant, drink, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants/" + participant.id + "/drinks";
            drink.timestamp = (new Date()).toISOString();
            http.post(url, $.param(drink)).success(callbackSuccess);
        };

        this.addDrinkToCurrentUser = function (drink, callbackSuccess) {
            var url = this._baseUrl + "/profile/drinks";
            drink.timestamp = (new Date()).toISOString();
            http.post(url, $.param(drink)).success(callbackSuccess);
        };

        this.addRegisteredUserToParty = function (partyId, email, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants";
            http.post(url, $.param({ email: email })).success(callbackSuccess);
        };

        this.addGuestToParty = function (partyId, guest, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants";
            http.post(url, $.param(guest)).success(callbackSuccess);
        };

        this.removeUser = function (partyId, participant, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants/" + participant.id;
            http.delete(url).success(callbackSuccess);
        };

        this.removeUser = function (partyId, participant, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants/" + participant.id;
            http.delete(url).success(callbackSuccess);
        };

        this.removeDrink = function (drinkId, callbackSuccess) {
            var url = this._baseUrl + "/profile/drinks/" + drinkId;
            http.delete(url).success(callbackSuccess);
        };

        this.addParty = function (partyName, callbackSuccess) {
            var url = this._baseUrl + "/parties";
            http.post(url, $.param({ name: partyName })).success(callbackSuccess);
        };

        this.removeParticipant = function (party, participant, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + party.id + "/participants/" + participant.id;
            http.delete(url).success(callbackSuccess);
        };
    }


    function SoundService(win) {
        var doc = win.document;
        var pool = [];
        var unlocked = false;

        function resolveFormat(probe) {
            if (win.navigator.userAgent.indexOf("Opera M") !== -1) { // stupid buggy opera mobile
                return { type: 'audio/wav', files: ['/static/sounds/7.wav'] };
            }

            var ogg = probe.canPlayType('audio/ogg; codecs="vorbis"');
            var mp3 = probe.canPlayType('audio/mpeg; codecs="mp3"');
            var files = [];
            for (var i = 1; i <= 7; i++) {
                files.push("/static/sounds/" + i);
            }

            if (ogg === "probably" || ogg === "maybe") {
                return { type: 'audio/ogg', files: files.map(function (f) { return f + '.ogg'; }) };
            } else if (mp3 === "probably" || mp3 === "maybe") {
                return { type: 'audio/mpeg', files: files.map(function (f) { return f + '.mp3'; }) };
            }
            return null;
        }

        function build() {
            var probe = doc.createElement("audio");
            if (!probe.canPlayType) {
                // no html5 audio support
                return;
            }

            var format = resolveFormat(probe);
            if (!format) {
                return;
            }

            pool = format.files.map(function (src) {
                var audio = doc.createElement("audio");
                var source = doc.createElement('source');
                source.type = format.type;
                source.src = src;
                audio.appendChild(source);
                return audio;
            });
        }

        // iOS Safari only allows audio.play() unprompted when it runs synchronously
        // inside a user gesture handler. playSound() is normally triggered later,
        // from an async API response, so it gets silently blocked on iPhone.
        // Unlocking each pooled <audio> element once during the first real tap
        // lets later script-triggered play() calls on those same elements succeed.
        function unlock() {
            if (unlocked) return;
            unlocked = true;
            pool.forEach(function (audio) {
                var playPromise = audio.play();
                audio.pause();
                audio.currentTime = 0;
                if (playPromise && playPromise.catch) {
                    playPromise.catch(function () {});
                }
            });
        }

        function addUnlockListener(evt) {
            doc.addEventListener(evt, function onUnlock() {
                doc.removeEventListener(evt, onUnlock);
                unlock();
            }, true);
        }

        build();
        addUnlockListener('touchend');
        addUnlockListener('click');

        this.playSound = function () {
            if (!pool.length) {
                return;
            }
            var audio = pool[Math.floor(Math.random() * pool.length)];
            audio.currentTime = 0;
            var playPromise = audio.play();
            if (playPromise && playPromise.catch) {
                playPromise.catch(function () {});
            }
        };
    }


    function NotificationService() {
        $.pnotify.defaults.pnotify_history = false;

        this.success = function (title, text) {
            $.pnotify({
                title: title,
                text: text,
                type: 'success'
            });
        };
    }


    /**
     * RyyppyAPI object is registered as an Angular service so that controllers or
     * other services can depend on it.
     */
    angular.module('ryyppy.services', [], function ($provide) {
        $provide.factory('RyyppyAPI', ['$http', function ($http) {
            return new RyyppyAPI($http);
        }]);

        $provide.factory('Sound', ['$window', function ($window) {
            return new SoundService($window);
        }]);

        $provide.factory('Notify', function () {
            return new NotificationService();
        });
    });
})(angular);

