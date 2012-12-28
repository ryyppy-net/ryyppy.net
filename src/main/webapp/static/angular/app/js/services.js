'use strict';

/**
 * RyyppyAPI is used to handle all API operations. It depends on Angular $http
 * service.
 */
function RyyppyAPI(http) {
    this._baseUrl = "/API/v2";

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