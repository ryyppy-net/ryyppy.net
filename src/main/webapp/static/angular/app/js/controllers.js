'use strict';

/* Controllers */


function MyCtrl1($scope, $http) {
    $http.get("/API/v2/parties").success(function (data) {
        $scope.parties = data;
    });
}
MyCtrl1.$inject = ['$scope', '$http'];


function MyCtrl2($scope, $http, $routeParams, $timeout) {
    $http.get("/API/v2/parties/" + $routeParams.partyId).success(function (data) {
        $scope.party = data;
    });

    (function tick() {
        $http.get("/API/v2/parties/" + $routeParams.partyId + "/participants").success(function (data) {
            $scope.participants = data;
        });
        console.log("Polling...");
        $timeout(tick, 5000);
    })();

    $scope.addDrink = function (participant) {
        console.log("Adding drink to " + participant.name);
        $http.post("/API/v2/parties/" + $routeParams.partyId + "/participants/" + participant.id + "/drinks", {"volume": 0.33, "alcohol": 0.5, "timestamp": null}).success(function (data) {
            console.log("Added drink.");
        });
    };
}
MyCtrl2.$inject = ['$scope', '$http', '$routeParams', '$timeout'];
