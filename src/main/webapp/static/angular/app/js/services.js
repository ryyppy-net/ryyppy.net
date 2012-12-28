'use strict';

(function (angular) {
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

        this.getParties = function (callbackSuccess) {
            http.get(this._baseUrl + "/parties").success(callbackSuccess);
        };

        this.getParty = function (partyId, callbackSuccess) {
            http.get(this._baseUrl + "/parties/" + partyId).success(callbackSuccess);
        };

        this.getPartyParticipants = function (partyId, callbackSuccess) {
            http.get(this._baseUrl + "/parties/" + partyId + "/participants").success(callbackSuccess);
        };

        this.addDrink = function (partyId, participant, drink, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants/" + participant.id + "/drinks";
            http.post(url, drink).success(callbackSuccess);
        };

        this.addRegisteredUserToParty = function (partyId, email, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants";
            http.post(url, $.param({ email: email }), callbackSuccess);
        };

        this.addGuestToParty = function (partyId, guest, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants";
            http.post(url, $.param(guest)).success(callbackSuccess);
        };

        this.removeUser = function (partyId, participant, callbackSuccess) {
            var url = this._baseUrl + "/parties/" + partyId + "/participants/" + participant.id;
            http.delete(url).success(callbackSuccess);
        }
    }


    function SoundService(win) {
        this.playSound = function () {
            var audio = win.document.createElement("audio");
            var source = win.document.createElement('source');
            var filename = "/static/sounds/" + Math.floor(Math.random() * 7 + 1);

            if (!audio.canPlayType) return; // no html5 audio support

            if (win.navigator.userAgent.indexOf("Opera M") !== -1) { // stupid buggy opera mobile
                source.type= 'audio/wav';
                source.src= '/static/sounds/7.wav';
                audio.appendChild(source);
                audio.play();
                return;
            }

            var ogg = audio.canPlayType('audio/ogg; codecs="vorbis"');
            var mp3 = audio.canPlayType('audio/mpeg; codecs="mp3"');
            if (ogg == "probably" || ogg == "maybe") {
                source.type= 'audio/ogg';
                source.src= filename + '.ogg';
            } else if (mp3 == "probably" || mp3 == "maybe") {
                source.type= 'audio/mpeg';
                source.src= filename + '.mp3';
            }

            if (source.src.length > 1) {
                audio.appendChild(source);
                audio.play();
            }
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
    });
})(angular);

