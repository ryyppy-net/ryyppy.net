'use strict';

/**
 * RyyppyAPI is used to handle all API operations. It depends on Angular $http
 * service.
 */
function RyyppyAPI(http) {
    this.getProfile = function (callbackSuccess) {
        http.get("/API/v2/profile").success(callbackSuccess);
    };

    this.getParties = function (callbackSuccess) {
        http.get("/API/v2/parties").success(callbackSuccess);
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