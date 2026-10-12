'use strict';

function GeneralPartyAdminPageCtrl($location, RyyppyAPI, Notify) {
    var self = this;

    this.refreshProfile = function () {
        RyyppyAPI.getProfile().then(function (data) {
            self.participant = data;
        });
    };

    this.refreshParties = function () {
        RyyppyAPI.getParties().then(function (data) {
            self.parties = data;
        });
    };

    this.addParty = function () {
        RyyppyAPI.addParty(self.partyName).then(function (party) {
            $location.path('/party/' + party.id);
        });
    };

    this.removeParticipant = function (party, participant) {
        RyyppyAPI.removeParticipant(party.id, participant).then(function (data) {
            Notify.success("Nyt jo kotiin?", "Lähdit bileistä " + party.name + ".");
            self.refreshParties();
        });
    };

    this.partySort = function (party) {
        return new Date(party.startTime).getTime();
    };

    this.$onInit = function () {
        self.refreshProfile();
        self.refreshParties();
    };
}

GeneralPartyAdminPageCtrl.$inject = ['$location', 'RyyppyAPI', 'Notify'];

angular.module('ryyppy').component('generalPartyAdminPage', {
    controller: GeneralPartyAdminPageCtrl,
    template: `
<user-menu active="general-party-admin"></user-menu>

<div class="container-fluid">
    <div class="row-fluid">
        <div class="offset1 span10">
            <h1>Lisää uudet bileet</h1>

            <form class="form-horizontal" ng-submit="$ctrl.addParty()">
                <div class="control-group">
                    <label class="control-label" for="partyName">Bileiden nimi</label>
                    <div class="controls">
                        <input id="partyName" type="text" ng-model="$ctrl.partyName" />
                    </div>
                </div>

                <div class="control-group">
                    <div class="controls">
                        <button type="submit" class="btn btn-success">Lisää bileet</button>
                    </div>
                </div>
            </form>

            <div ng-show="$ctrl.parties.length > 0">
                <h2>Bileesi</h2>

                <div ng-repeat="party in $ctrl.parties | orderBy:$ctrl.partySort:true">
                    <div class="row-fluid">
                        <div class="span10 party color{{ ($index % 12) + 1 }}">
                            <div>{{ ::party.name }} <small style="float: right;"><span class="hidden-phone">Alkamisaika:</span> {{ ::party.startTime | formatDateTime }}</small></div>
                            <ul class="inline" ng-show="party.participants.length > 0" style="margin-top: 5px; margin-bottom: 0px;">
                                <li ng-repeat="participantPreview in party.participants"><img ng-src="{{ participantPreview.profilePictureUrl }}&size=30" title="{{ participantPreview.name }}" /></li>
                            </ul>
                        </div>
                        <div class="span2">
                            <button ng-click="$ctrl.removeParticipant(party, $ctrl.participant)" class="btn btn-danger">Poistu bileistä</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</div>`
});
