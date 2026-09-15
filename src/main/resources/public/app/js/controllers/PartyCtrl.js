"use strict";

function PartyCtrl($scope, $routeParams, Poller, RyyppyAPI) {
    var self = this;
    var stopPolling = null;


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
                data[i].color = (i % 12) + 1;
            }

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
        stopPolling = Poller.start(self.refreshParticipants, 60000);
    };

    this.endPolling = function () {
        if (stopPolling) {
            stopPolling();
            stopPolling = null;
        }
    };


    // Initialization:
    $scope.active = "party";
    this.refreshParty();

    self.startPolling();
    $scope.$on('$destroy', function () {
        self.endPolling();
    });

    $scope.$on('drinkAdded', function () {
        self.endPolling();
        self.startPolling();
    });
}

PartyCtrl.$inject = ['$scope', '$routeParams', 'Poller', 'RyyppyAPI'];
