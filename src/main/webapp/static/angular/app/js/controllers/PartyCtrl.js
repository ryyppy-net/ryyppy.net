"use strict";

function PartyCtrl($scope, $routeParams, $timeout, RyyppyAPI) {
    var self = this;
    $scope.active = "party";

    RyyppyAPI.getParty($routeParams.partyId, function (data) {
        $scope.party = data;
    });

    (function tick() {
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
        console.log("Polling...");
        self.timeoutPromise = $timeout(tick, 60000);
    })();

    $scope.$on('$destroy', function cleanup() {
        $timeout.cancel(self.timeoutPromise);
    });
}

PartyCtrl.$inject = ['$scope', '$routeParams', '$timeout', 'RyyppyAPI'];
