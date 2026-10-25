/*global angular */

(function (angular) {
    'use strict';

    /**
     * RyyppyAPI is used to handle all API operations. It depends on Angular $http
     * service.
     */
    function RyyppyAPI(http, serialize) {
        var baseUrl = "/API/v2";

        // AngularJS would prefer to use application/json but as our backend
        // doesn't support this yet, fallback type must be set.
        http.defaults.headers.post["Content-Type"] = "application/x-www-form-urlencoded";
        http.defaults.headers.put["Content-Type"] = "application/x-www-form-urlencoded";

        function data(response) {
            return response.data;
        }

        function participantsUrl(partyId) {
            return baseUrl + "/parties/" + partyId + "/participants";
        }

        // On the party page every drinker, self included, is a party
        // participant; on the dashboard the only drinker is the current user.
        function drinksUrl(drinker) {
            if (drinker.type === 'participant') {
                return participantsUrl(drinker.partyId) + "/" + drinker.id + "/drinks";
            }
            return baseUrl + "/profile/drinks";
        }

        this.getProfile = function () {
            return http.get(baseUrl + "/profile").then(data);
        };

        this.updateProfile = function (profile) {
            return http.post(baseUrl + "/profile", serialize(profile)).then(data);
        };

        this.getOwnDrinks = function () {
            // The user page lists the five newest; OwnDrinksView embeds and returns the same five.
            return http.get(baseUrl + "/profile/drinks", {params: {latest: 5}}).then(data);
        };

        this.getDrinkHistory = function () {
            return http.get(baseUrl + "/profile/drink-history", {transformResponse: angular.identity}).then(data);
        };

        this.getParties = function (limit) {
            return http.get(baseUrl + "/parties", {params: {limit: limit}}).then(data);
        };

        this.getParty = function (partyId) {
            return http.get(baseUrl + "/parties/" + partyId).then(data);
        };

        this.getPartyParticipants = function (partyId) {
            return http.get(participantsUrl(partyId)).then(data);
        };

        this.getPartyInvitations = function (partyId) {
            return http.get(baseUrl + "/parties/" + partyId + "/invitations").then(data);
        };

        this.inviteUser = function (userId, partyId) {
            return http.post(baseUrl + "/parties/" + partyId + "/invitations", serialize({ userId : userId })).then(data);
        };

        this.addDrink = function (drinker, drink) {
            var params = {volume: drink.volume, alcohol: drink.alcohol, timestamp: (new Date()).toISOString()};
            return http.post(drinksUrl(drinker), serialize(params)).then(data);
        };

        this.changeDrink = function (drinker, drinkId, drink) {
            var params = {volume: drink.volume, alcohol: drink.alcohol};
            return http.put(drinksUrl(drinker) + "/" + drinkId, serialize(params)).then(data);
        };

        this.deleteDrink = function (drinker, drinkId) {
            return http.delete(drinksUrl(drinker) + "/" + drinkId).then(data);
        };

        this.addRegisteredUserToParty = function (partyId, email) {
            return http.post(participantsUrl(partyId), serialize({ email: email })).then(data);
        };

        this.addGuestToParty = function (partyId, guest) {
            return http.post(participantsUrl(partyId), serialize(guest)).then(data);
        };

        this.removeParticipant = function (partyId, participant) {
            return http.delete(participantsUrl(partyId) + "/" + participant.id).then(data);
        };

        this.addParty = function (partyName) {
            return http.post(baseUrl + "/parties", serialize({ name: partyName })).then(data);
        };
    }


    /**
     * Thin wrapper over the shared player in /static/js/sound.js, which
     * preloads the drink sounds via Howler so playback is instant.
     */
    function SoundService(win) {
        this.playSound = function () {
            if (win.RyyppySound) {
                win.RyyppySound.play();
            }
        };
    }


    /**
     * Data the server embeds into the page on load as window.__INITIAL_<NAME>__
     * (templates/app/index.html), which seeds the stores in stores.js.
     */
    function InitialData(win) {
        this.get = function (name) {
            var value = win['__INITIAL_' + name + '__'];
            return value === undefined ? null : value;
        };
    }


    /**
     * Poller repeats a tick function on an interval, first after one interval,
     * but skips ticks (and stops scheduling further ones) while the tab is in
     * the background, per the Page Visibility API. When the tab becomes
     * visible again it ticks immediately and resumes the interval, so
     * background tabs don't keep polling the backend.
     */
    function Poller($timeout, $document) {
        this.start = function (tick, intervalMs) {
            var doc = $document[0];
            var timeoutPromise = null;

            function scheduleNext() {
                timeoutPromise = $timeout(function () {
                    timeoutPromise = null;
                    tick();
                    scheduleNext();
                }, intervalMs);
            }

            function resume() {
                tick();
                scheduleNext();
            }

            function onVisibilityChange() {
                if (doc.hidden) {
                    // Cancel the in-flight timer outright so a tick already
                    // scheduled before backgrounding doesn't still fire.
                    $timeout.cancel(timeoutPromise);
                    timeoutPromise = null;
                } else if (!timeoutPromise) {
                    resume();
                }
            }

            doc.addEventListener('visibilitychange', onVisibilityChange);
            if (!doc.hidden) {
                scheduleNext();
            }

            return function stop() {
                $timeout.cancel(timeoutPromise);
                doc.removeEventListener('visibilitychange', onVisibilityChange);
            };
        };
    }


    function NotificationService($timeout) {
        var DISPLAY_MS = 8000;
        var nextId = 1;
        var timers = {};
        var self = this;

        this.toasts = [];

        this.success = function (title, text) {
            var toast = {id: nextId++, title: title, text: text};
            self.toasts.push(toast);
            timers[toast.id] = {remaining: DISPLAY_MS};
            self.resume(toast);
            return toast;
        };

        // Withdraws a toast whose subject is gone, e.g. an undone drink.
        this.remove = function (toast) {
            toast.withdrawn = true;
            self.dismiss(toast);
        };

        this.dismiss = function (toast) {
            var timer = timers[toast.id];
            if (timer) {
                $timeout.cancel(timer.handle);
                delete timers[toast.id];
            }
            // Next tick, so the withdrawn class is rendered before ng-leave starts.
            $timeout(function () {
                var index = self.toasts.indexOf(toast);
                if (index >= 0) {
                    self.toasts.splice(index, 1);
                }
            }, 0);
        };

        this.pause = function (toast) {
            var timer = timers[toast.id];
            if (timer && timer.handle) {
                $timeout.cancel(timer.handle);
                timer.handle = null;
                timer.remaining -= Date.now() - timer.startedAt;
            }
        };

        this.resume = function (toast) {
            var timer = timers[toast.id];
            if (timer && !timer.handle) {
                timer.startedAt = Date.now();
                timer.handle = $timeout(function () {
                    self.dismiss(toast);
                }, timer.remaining);
            }
        };
    }


    /**
     * RyyppyAPI object is registered as an Angular service so that controllers or
     * other services can depend on it.
     */
    angular.module('ryyppy.services', [], function ($provide) {
        $provide.factory('RyyppyAPI', ['$http', '$httpParamSerializerJQLike', function ($http, $httpParamSerializerJQLike) {
            return new RyyppyAPI($http, $httpParamSerializerJQLike);
        }]);

        $provide.factory('Sound', ['$window', function ($window) {
            return new SoundService($window);
        }]);

        $provide.factory('InitialData', ['$window', function ($window) {
            return new InitialData($window);
        }]);

        $provide.factory('Poller', ['$timeout', '$document', function ($timeout, $document) {
            return new Poller($timeout, $document);
        }]);

        $provide.factory('Notify', ['$timeout', function ($timeout) {
            return new NotificationService($timeout);
        }]);
    });
})(angular);

