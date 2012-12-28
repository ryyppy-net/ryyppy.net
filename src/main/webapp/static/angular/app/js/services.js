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

    /**
     * RyyppyAPI object is registered as an Angular service so that controllers or
     * other services can depend on it.
     */
    angular.module('ryyppy.services', [], function ($provide) {
        $provide.factory('RyyppyAPI', ['$http', function ($http) {
            return new RyyppyAPI($http);
        }]);
    });
})(angular);

