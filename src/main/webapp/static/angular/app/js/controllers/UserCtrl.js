"use strict";

function UserCtrl($scope, $timeout, RyyppyAPI, Notify) {
    var self = this;
    $scope.active = 'user';

    this.refreshProfile = function () {
        RyyppyAPI.getProfile(function (data) {
            data.type = 'profile';
            data.color = 1;
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

    $scope.addParty = function () {
        RyyppyAPI.addParty($scope.partyName, function () {
            self.refreshParties();
            Notify.success("New party added", "Let's get this party started!")
        });
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

UserCtrl.$inject = ['$scope', '$timeout', 'RyyppyAPI', 'Notify'];