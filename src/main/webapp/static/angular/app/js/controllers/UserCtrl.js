"use strict";

function UserCtrl($scope, RyyppyAPI) {
    RyyppyAPI.getProfile(function (data) {
        $scope.profile = data;
    });

    RyyppyAPI.getParties(function (data) {
        $scope.parties = data;
    });
}

UserCtrl.$inject = ['$scope', 'RyyppyAPI'];