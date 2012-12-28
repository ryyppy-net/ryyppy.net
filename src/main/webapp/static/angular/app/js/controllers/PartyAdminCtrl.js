"use strict";

function PartyAdminCtrl($scope, $routeParams, RyyppyAPI) {
    $scope.active = "admin";

    RyyppyAPI.getParty($routeParams.partyId, function (data) {
        $scope.party = data;
    });
    RyyppyAPI.getPartyParticipants($routeParams.partyId, function (data) {
        $scope.participants = data;
    });

    $scope.email = null;
    $scope.name = null;
    $scope.sex = 'MALE';
    $scope.weight = null;

    $scope.addUser = function () {
        if ($scope.selectedUserTypeId == 1) {
            RyyppyAPI.addRegisteredUserToParty($routeParams.partyId, $scope.email, function (data) {
                alert($scope.email);
            });
        }
        else {
            var guest = { name: $scope.name, sex: $scope.sex, weight: $scope.weight };
            RyyppyAPI.addGuestToParty($routeParams.partyId, guest, function (data) {
                alert($scope.name + $scope.sex + $scope.weight);
            });
        }
    };

    $scope.removeUser = function (participant) {
        RyyppyAPI.removeUser($routeParams.partyId, participant, function (data) {
            alert($scope.email);
        });
    };

    $scope.userTypes = [
        {TypeId: 1, TypeName: 'Rekisteröitynyt käyttäjä'},
        {TypeId: 2, TypeName: 'Vieras'}
    ];
    $scope.selectedUserTypeId = 1;
}

PartyAdminCtrl.$inject = ['$scope', '$routeParams', 'RyyppyAPI'];