'use strict';

/* Controllers */


function UserCtrl($scope, $http) {
    $http.get("/API/v2/profile").success(function (data) {
        $scope.profile = data;
    });

    $http.get("/API/v2/parties").success(function (data) {
        $scope.parties = data;
    });
}
UserCtrl.$inject = ['$scope', '$http'];


function PartyCtrl($scope, $http, $routeParams, $timeout) {
    var self = this;

    $http.get("/API/v2/parties/" + $routeParams.partyId).success(function (data) {
        $scope.party = data;
    });

    (function tick() {
        $http.get("/API/v2/parties/" + $routeParams.partyId + "/participants").success(function (data) {
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

    $scope.addDrink = function (participant) {
        console.log("Adding drink to " + participant.name);
        $http.post("/API/v2/parties/" + $routeParams.partyId + "/participants/" + participant.id + "/drinks", {"volume": 0.33, "alcohol": 0.5, "timestamp": null}).success(function (data) {
            console.log("Added drink.");
        });
    };
}
PartyCtrl.$inject = ['$scope', '$http', '$routeParams', '$timeout'];
