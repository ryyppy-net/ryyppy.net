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


    /**
     * Thin wrapper over the shared player in /static/js/sound.js, which
     * preloads and decodes the drink sounds so playback is instant.
     */
    function SoundService(win) {
        this.playSound = function () {
            if (win.RyyppySound) {
                win.RyyppySound.play();
            }
        };
    }


    /**
     * Poller repeats a tick function on an interval, but skips ticks (and
     * stops scheduling further ones) while the tab is in the background, per
     * the Page Visibility API. When the tab becomes visible again it ticks
     * immediately and resumes the interval, so background tabs don't keep
     * polling the backend.
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
                resume();
            }

            return function stop() {
                $timeout.cancel(timeoutPromise);
                doc.removeEventListener('visibilitychange', onVisibilityChange);
            };
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

        $provide.factory('Poller', ['$timeout', '$document', function ($timeout, $document) {
            return new Poller($timeout, $document);
        }]);

        $provide.factory('Notify', function () {
            return new NotificationService();
        });
    });
})(angular);

