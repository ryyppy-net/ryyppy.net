"use strict";

function PartyCtrl($scope, $routeParams, $timeout, RyyppyAPI) {
    var self = this;


    this.refreshParty = function () {
        RyyppyAPI.getParty($routeParams.partyId, function (data) {
            $scope.party = data;
        });
    };

    this.refreshParticipants = function () {
        RyyppyAPI.getPartyParticipants($routeParams.partyId, function (data) {
            // Should this be added in backend?
            for (var i = 0; i < data.length; i++) {
                data[i].type = 'participant';
                data[i].partyId = $routeParams.partyId;
            }

            $scope.participants = data;

            var rowsAmount = Math.ceil(data.length / 3);
            var rows = new Array(rowsAmount);
            for (var i = 0; i < rowsAmount; i++) {
                var colsAmount = Math.min(3, data.length - i * 3);
                var cols = new Array(colsAmount);
                rows[i] = cols;
                for (var j = 0; j < cols.length; j++) {
                    cols[j] = data[i * 3 + j];
                }
            }
            $scope.rows = rows;
        });
    };

    this.startPolling = function () {
        (function tick() {
            self.refreshParticipants();
            console.log("Polling...");
            self.timeoutPromise = $timeout(tick, 60000);
        })();
    };

    this.endPolling = function () {
        $timeout.cancel(self.timeoutPromise);
    };


    // Initialization:
    $scope.active = "party";
    this.refreshParty();

    self.startPolling();
    $scope.$on('$destroy', function () {
        self.endPolling();
    });

    $scope.$on('drinkAdded', function () {
        self.refreshParticipants();
    });
}

PartyCtrl.$inject = ['$scope', '$routeParams', '$timeout', 'RyyppyAPI'];
