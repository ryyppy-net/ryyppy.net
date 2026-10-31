(function (angular) {
    'use strict';

    function GeneralPartyAdminPageCtrl($location, $interval, PartiesStore, PartyStore, ProfileStore, Notify, PartyTime) {
        var self = this;
        var releaseParties = null;
        var refresh = null;

        this.now = Date.now();
        this.days = 1;

        this.parties = PartiesStore.parties;
        this.partiesShown = PartiesStore.shown;
        this.hasMoreParties = PartiesStore.hasMore;
        this.showMoreParties = PartiesStore.showMore;
        this.profile = ProfileStore.profile;

        this.addParty = function () {
            PartiesStore.addParty(self.partyName, self.days).then(function (party) {
                $location.path('/party/' + party.id);
            });
        };

        this.leaveParty = function (party) {
            PartyStore.leave(party.id, self.profile.data).then(function () {
                Notify.success("Nyt jo kotiin?", "Lähdit bileistä " + party.name + ".");
            });
        };

        this.setDays = function (days) {
            self.days = days;
        };

        this.endText = function () {
            return PartyTime.at(PartyTime.after(self.now, self.days));
        };

        this.status = function (party) {
            return PartyTime.status(party.startTime, party.endTime, self.now);
        };

        this.range = function (party) {
            return PartyTime.range(party.startTime, party.endTime);
        };

        this.partySort = function (party) {
            return new Date(party.startTime).getTime();
        };

        this.$onInit = function () {
            self.profile.load();
            releaseParties = PartiesStore.parties.use();
            refresh = $interval(function () { self.now = Date.now(); }, 60000);
        };

        this.$onDestroy = function () {
            releaseParties();
            $interval.cancel(refresh);
        };
    }

    GeneralPartyAdminPageCtrl.$inject = ['$location', '$interval', 'PartiesStore', 'PartyStore', 'ProfileStore', 'Notify', 'PartyTime'];

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
                        <span class="control-label">Kesto</span>
                        <div class="controls">
                            <day-stepper days="$ctrl.days" on-change="$ctrl.setDays(days)"></day-stepper>
                            <span class="duration-summary">alkaa nyt, päättyy {{ $ctrl.endText() }}</span>
                        </div>
                    </div>

                    <div class="control-group">
                        <div class="controls">
                            <button type="submit" class="btn btn-success">Lisää bileet</button>
                        </div>
                    </div>
                </form>

                <div ng-if="$ctrl.parties.data.length > 0">
                    <h2>Bileesi</h2>

                    <div ng-repeat="party in $ctrl.parties.data | orderBy:[$ctrl.partySort, 'id']:true | limitTo:$ctrl.partiesShown() track by party.id">
                        <div class="row-fluid">
                            <div class="span10 party color{{ ($index % 12) + 1 }}">
                                <div>
                                    {{ ::party.name }}
                                    <span class="status-badge" ng-class="$ctrl.status(party).state == 'ended' ? 'status-badge-ended' : 'status-badge-running'">{{ $ctrl.status(party).state == 'ended' ? 'PÄÄTTYNYT' : 'KÄYNNISSÄ' }}</span>
                                    <small style="float: right;">{{ $ctrl.range(party) }}</small>
                                </div>
                                <ul class="inline" ng-if="party.participants.length > 0" style="margin-top: 5px; margin-bottom: 0px;">
                                    <li ng-repeat="participantPreview in party.participants"><img ng-src="{{ participantPreview.profilePictureUrl }}&size=30" title="{{ participantPreview.name }}" /></li>
                                </ul>
                                <div class="party-progress" ng-if="$ctrl.status(party).state != 'ended'"><div class="party-progress-fill" ng-style="{width: ($ctrl.status(party).progress * 100) + '%'}"></div></div>
                            </div>
                            <div class="span2">
                                <button ng-click="$ctrl.leaveParty(party)" class="btn btn-danger">Poistu bileistä</button>
                            </div>
                        </div>
                    </div>
                    <button id="morePartiesButton" class="btn" ng-if="$ctrl.hasMoreParties()" ng-click="$ctrl.showMoreParties()">Näytä lisää bileitä</button>
                </div>
            </div>
        </div>
    </div>`
    });
})(angular);
