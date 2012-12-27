'use strict';

/* Controllers */


function MyCtrl1($scope, $http) {
    $http.get("/API/v2/parties").success(function (data) {
        $scope.parties = data;
    });
}
MyCtrl1.$inject = ['$scope', '$http'];


function MyCtrl2($scope, $http, $routeParams) {
    $http.get("/API/v2/parties/" + $routeParams.partyId).success(function (data) {
        $scope.party = data;
    });
    $http.get("/API/v2/parties/" + $routeParams.partyId + "/participants").success(function (data) {
        $scope.participants = data;
    });
}
MyCtrl2.$inject = ['$scope', '$http', '$routeParams'];
