"use strict";

function UserCtrl($scope, Poller, RyyppyAPI, Notify) {
    var self = this;
    var stopPolling = null;
    var graph = null;
    $scope.active = 'user';

    function applyProfile(data) {
        data.type = 'profile';
        data.color = 1;
        $scope.participants = [data];

        setTimeout(function () {
            var container = document.getElementById('historyGraph');
            if (!container) {
                return;
            }
            if (!graph) {
                graph = new UserHistoryGraph($scope.participants[0], container);
            }
            graph.update();
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
        stopPolling = Poller.start(function () {
            self.refreshProfile();
            self.refreshOwnDrinks();
        }, 60000);
    };

    this.endPolling = function () {
        if (stopPolling) {
            stopPolling();
            stopPolling = null;
        }
    };

    $scope.partySort = function (party) {
        return new Date(party.startTime).getTime();
    };

    $scope.drinkSort = function (drink) {
        return new Date(drink.timestamp).getTime();
    };

    this.refreshParties();

    this.startPolling();

    $scope.drinksChanged = function () {
        self.endPolling();
        self.startPolling();
    };

    $scope.$on('$destroy', function () {
        self.endPolling();
        if (graph) {
            graph.destroy();
        }
    });

    $scope.removeDrink = function (drink) {
        RyyppyAPI.removeDrink(drink.id, function () {
            self.endPolling();
            self.startPolling();
        });
    };
}

UserCtrl.$inject = ['$scope', 'Poller', 'RyyppyAPI', 'Notify'];

angular.module('ryyppy').controller('UserCtrl', UserCtrl);
