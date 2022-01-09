"use strict";

function UserCtrl($scope, $timeout, RyyppyAPI, Notify) {
    var self = this;
    $scope.active = 'user';

    this.refreshProfile = function () {
        RyyppyAPI.getProfile(function (data) {
            data.type = 'profile';
            data.color = 1;
            $scope.participants = [data];

            setTimeout(function () {
                var graph = new UserHistoryGraph($scope.participants[0], $("#historyGraph"));
                graph.update();
                graph.render();
            }, 0);
        });
    };

    this.refreshParties = function () {
        RyyppyAPI.getParties(function (data) {
            $scope.parties = data;
        });
    };

    this.refreshOwnDrinks = function () {
        RyyppyAPI.getOwnDrinks(function (data) {
            $scope.drinks = data;
        });
    };

    this.startPolling = function () {
        (function tick() {
            self.refreshProfile();
            self.refreshOwnDrinks();
            self.timeoutPromise = $timeout(tick, 60000);
        })();
    };

    this.endPolling = function () {
        $timeout.cancel(self.timeoutPromise);
    };

    $scope.partySort = function (party) {
        return moment(party.startTime, 'MMM DD, YYYY h:mm:ss A');
    };

    $scope.drinkSort = function (drink) {
        return moment(drink.timestamp);
    };

    this.refreshProfile();
    this.refreshOwnDrinks();
    this.refreshParties();

    this.startPolling();

    $scope.$on('drinkAdded', function () {
        self.refreshProfile();
        self.refreshOwnDrinks();
    });

    $scope.$on('$destroy', function () {
        self.endPolling();
    });

    $scope.removeDrink = function (drink) {
        RyyppyAPI.removeDrink(drink.id, function () {
            self.refreshProfile();
            self.refreshOwnDrinks();
        });
    };
}

UserCtrl.$inject = ['$scope', '$timeout', 'RyyppyAPI', 'Notify'];