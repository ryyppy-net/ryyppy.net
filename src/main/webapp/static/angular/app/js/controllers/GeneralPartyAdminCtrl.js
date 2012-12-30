"use strict";

function GeneralPartyAdminCtrl($scope, $routeParams, RyyppyAPI, Notify) {
    var self = this;

    this.refreshProfile = function () {
        RyyppyAPI.getProfile(function (data) {
            $scope.participant = data;
        });
    };

    this.refreshParties = function () {
        RyyppyAPI.getParties(function (data) {
            $scope.parties = data;
        });
    };

    $scope.addParty = function () {
        RyyppyAPI.addParty($scope.partyName, function () {
            self.refreshParties();
            Notify.success("New party added", "Let's get this party started!");
            $scope.partyName = "";
        });
    };

    $scope.removeParticipant = function (party, participant) {
        RyyppyAPI.removeParticipant(party, participant, function (data) {
            Notify.success("Poistuttu bileistä", "Sinut poistettiin bileistä " + party.name + "!");
            self.refreshParties();
        });
    };

    $scope.partySort = function (party) {
        return moment(party.startTime, 'MMM DD, YYYY h:mm:ss A');
    };

    $scope.active = 'general-party-admin';

    this.refreshProfile();
    this.refreshParties();
}

GeneralPartyAdminCtrl.$inject = ['$scope', '$routeParams', 'RyyppyAPI', 'Notify'];
