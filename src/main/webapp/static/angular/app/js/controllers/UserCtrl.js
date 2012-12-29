"use strict";

function UserCtrl($scope, RyyppyAPI) {
    RyyppyAPI.getProfile(function (data) {
        data.type = 'profile';
        $scope.participant = data;
    });

    RyyppyAPI.getParties(function (data) {
        $scope.parties = data;
    });
}

UserCtrl.$inject = ['$scope', 'RyyppyAPI'];