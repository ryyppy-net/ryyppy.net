"use strict";

function UserCtrl($scope, $timeout, RyyppyAPI, Notify) {
    var self = this;
    $scope.active = 'user';

    function applyProfile(data) {
        data.type = 'profile';
        data.color = 1;
        $scope.participants = [data];

        setTimeout(function () {
            var graph = new UserHistoryGraph($scope.participants[0], $("#historyGraph"));
            graph.update();
            graph.render();
        }, 0);
    }

    this.refreshProfile = function () {
        // The server embeds the profile into the page on load (see
        // app/index.jsp) so the very first refresh can skip the XHR. Once
        // used it's cleared so later polling ticks always hit the API.
        if (window.__INITIAL_PROFILE__) {
            var initialProfile = window.__INITIAL_PROFILE__;
            window.__INITIAL_PROFILE__ = null;
            applyProfile(initialProfile);
            return;
        }
        RyyppyAPI.getProfile(applyProfile);
    };

    this.refreshParties = function () {
        // See refreshProfile above - same skip-first-fetch pattern using data
        // embedded by the server (app/index.jsp).
        if (window.__INITIAL_PARTIES__) {
            var initialParties = window.__INITIAL_PARTIES__;
            window.__INITIAL_PARTIES__ = null;
            $scope.parties = initialParties;
            return;
        }
        RyyppyAPI.getParties(function (data) {
            $scope.parties = data;
        });
    };

    this.refreshOwnDrinks = function () {
        // See refreshProfile above - same skip-first-fetch pattern using data
        // embedded by the server (app/index.jsp).
        if (window.__INITIAL_DRINKS__) {
            var initialDrinks = window.__INITIAL_DRINKS__;
            window.__INITIAL_DRINKS__ = null;
            $scope.drinks = initialDrinks;
            return;
        }
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
        return moment(party.startTime);
    };

    $scope.drinkSort = function (drink) {
        return moment(drink.timestamp);
    };

    this.refreshParties();

    this.startPolling();

    $scope.$on('drinkAdded', function () {
        self.endPolling();
        self.startPolling();
    });

    $scope.$on('$destroy', function () {
        self.endPolling();
    });

    $scope.removeDrink = function (drink) {
        RyyppyAPI.removeDrink(drink.id, function () {
            self.endPolling();
            self.startPolling();
        });
    };
}

UserCtrl.$inject = ['$scope', '$timeout', 'RyyppyAPI', 'Notify'];