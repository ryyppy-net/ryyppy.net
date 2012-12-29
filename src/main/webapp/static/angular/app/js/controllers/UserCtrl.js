"use strict";

function UserCtrl($scope, RyyppyAPI) {
    var self = this;

    this.refreshProfile = function () {
        RyyppyAPI.getProfile(function (data) {
            data.type = 'profile';
            $scope.participant = data;
        });
    };

    this.refreshParties = function () {
        RyyppyAPI.getParties(function (data) {
            $scope.parties = data;
        });
    };


    this.refreshProfile();
    this.refreshParties();

    $scope.$on('drinkAdded', function () {
        self.refreshProfile();
    });
}

UserCtrl.$inject = ['$scope', 'RyyppyAPI'];