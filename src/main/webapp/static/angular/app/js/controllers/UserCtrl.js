"use strict";

function UserCtrl($scope, $timeout, RyyppyAPI) {
    var self = this;

    this.refreshProfile = function () {
        RyyppyAPI.getProfile(function (data) {
            data.type = 'profile';
            $scope.participants = [data];
        });
    };

    this.refreshParties = function () {
        RyyppyAPI.getParties(function (data) {
            $scope.parties = data;
        });
    };

    this.startPolling = function () {
        (function tick() {
            self.refreshProfile();
            self.timeoutPromise = $timeout(tick, 60000);
        })();
    };

    this.endPolling = function () {
        $timeout.cancel(self.timeoutPromise);
    };


    this.refreshProfile();
    this.refreshParties();

    this.startPolling();

    $scope.$on('drinkAdded', function () {
        self.refreshProfile();
    });

    $scope.$on('$destroy', function () {
        self.endPolling();
    });
}

UserCtrl.$inject = ['$scope', '$timeout', 'RyyppyAPI'];