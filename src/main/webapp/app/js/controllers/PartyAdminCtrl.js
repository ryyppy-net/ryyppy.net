"use strict";

function PartyAdminCtrl($scope, $routeParams, RyyppyAPI, Notify) {
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

    this._updatePartyInvitations = function () {
        RyyppyAPI.getPartyInvitations($routeParams.partyId, function (data) {
            $scope.invitations = data;
        });
    };

    this.addUser = function () {
        if ($scope.selectedUserTypeId == 2)
            self._addRegisteredUser($routeParams.partyId, $scope.email);
        if ($scope.selectedUserTypeId == 3)
            self._addGuestUser($scope.name, $scope.sex, $scope.weight);
    };

    this._addRegisteredUser = function (partyId, email) {
        RyyppyAPI.addRegisteredUserToParty(partyId, email, function (data) {
            Notify.success("Tervetuloa takaisin!", "Lisätty käyttäjä sähköpostiosoitteella " + email + ".");
            self._updatePartyParticipants();
            self._updatePartyInvitations();
        });
    };

    this._addGuestUser = function (name, sex, weight) {
        var guest = { name: name, sex: sex, weight: weight };
        RyyppyAPI.addGuestToParty($routeParams.partyId, guest, function (data) {
            Notify.success("Tervetuloa vieraalle!", "Lisätty vieras " + $scope.name + ".");
            self._updatePartyParticipants();
        });
    }

    $scope.inviteUser = function (invitation) {
        $scope.inviting = true;
        $scope.invitedFriend = invitation;
        RyyppyAPI.inviteUser(invitation.id, $routeParams.partyId, function () {
            $scope.invitedFriend = undefined;
            Notify.success("Pitkästä aikaa!", "Lisätty käyttäjä " + invitation.name + ".");
            self._updatePartyParticipants();
            self._updatePartyInvitations();
            $scope.inviting = false;
        });
    }

    this.removeUser = function (participant) {
        RyyppyAPI.removeUser($routeParams.partyId, participant, function (data) {
            Notify.success("ULOS!", "Heitettiin " + participant.name + " pihalle.");
            self._updatePartyParticipants();
            self._updatePartyInvitations();
        });
    };


    $scope.active = "admin";

    $scope.email = null;
    $scope.name = null;
    $scope.sex = 'MALE';
    $scope.weight = null;
    $scope.inviting = false;

    $scope.userTypes = [
        {TypeId: 1, TypeName: 'Vanha ystävä'},
        {TypeId: 2, TypeName: 'Rekisteröitynyt käyttäjä'},
        {TypeId: 3, TypeName: 'Vieras'}
    ];
    $scope.selectedUserTypeId = 1;

    $scope.addUser = this.addUser;
    $scope.removeUser = this.removeUser;

    this._updatePartyInformation();
    this._updatePartyParticipants();
    this._updatePartyInvitations();
}

PartyAdminCtrl.$inject = ['$scope', '$routeParams', 'RyyppyAPI', 'Notify'];
