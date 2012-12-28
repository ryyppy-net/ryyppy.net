"use strict";

function PartyAdminCtrl($scope, $routeParams, RyyppyAPI) {
    var self = this;

    this._updatePartyInformation = function () {
        RyyppyAPI.getParty($routeParams.partyId, function (data) {
            $scope.party = data;
        });
    };

    this._updatePartyParticipants = function () {
        RyyppyAPI.getPartyParticipants($routeParams.partyId, function (data) {
            $scope.participants = data;
        });
    };

    this.addUser = function () {
        if ($scope.selectedUserTypeId == 1)
            self._addRegisteredUser($routeParams.partyId, $scope.email);
        else
            self._addGuestUser($scope.name, $scope.sex, $scope.weight);
    };

    this._addRegisteredUser = function (partyId, email) {
        RyyppyAPI.addRegisteredUserToParty(partyId, email, function (data) {
            $scope.successMessage = "Lisätty käyttäjä sähköpostiosoitteella " + email + "!";
            self._updatePartyParticipants();
        });
    };

    this._addGuestUser = function (name, sex, weight) {
        var guest = { name: name, sex: sex, weight: weight };
        RyyppyAPI.addGuestToParty($routeParams.partyId, guest, function (data) {
            $scope.successMessage = "Lisätty vieras " + $scope.name + "!";
            self._updatePartyParticipants();
        });
    }

    this.removeUser = function (participant) {
        RyyppyAPI.removeUser($routeParams.partyId, participant, function (data) {
            $scope.successMessage = "Poistettu käyttäjä " + participant.name + "!";
            self._updatePartyParticipants();
        });
    };


    $scope.active = "admin";

    $scope.email = null;
    $scope.name = null;
    $scope.sex = 'MALE';
    $scope.weight = null;

    $scope.userTypes = [
        {TypeId: 1, TypeName: 'Rekisteröitynyt käyttäjä'},
        {TypeId: 2, TypeName: 'Vieras'}
    ];
    $scope.selectedUserTypeId = 1;

    $scope.addUser = this.addUser;
    $scope.removeUser = this.removeUser;

    this._updatePartyInformation();
    this._updatePartyParticipants();
}

PartyAdminCtrl.$inject = ['$scope', '$routeParams', 'RyyppyAPI'];