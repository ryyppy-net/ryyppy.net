'use strict';

/* Controllers */


function MyCtrl1($scope, $http)
{
    $http.get("/API/v2/parties").success(function (data) {
        $scope.parties = data;
    });
}
MyCtrl1.$inject = ['$scope', '$http'];


function MyCtrl2($scope, $routeParams) {
    $scope.partyId = $routeParams.partyId;
}
MyCtrl2.$inject = ['$scope', '$routeParams'];
