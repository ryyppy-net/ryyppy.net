(function (angular) {
    'use strict';

    function PartyPageCtrl($routeParams, $interval, PartyStore, PartyTime) {
        var self = this;
        var releaseParty = null;
        var releaseParticipants = null;
        var shownParticipants = null;
        var refresh = null;

        this.now = Date.now();

        this.store = PartyStore.get($routeParams.partyId);

        function showParticipants(participants) {
            // Should this be added in backend?
            var drinkers = participants.map(function (participant, i) {
                return angular.extend({type: 'participant', partyId: $routeParams.partyId, color: (i % 12) + 1}, participant);
            });

            var rows = [];
            for (var i = 0; i < drinkers.length; i += 3) {
                rows.push(drinkers.slice(i, i + 3));
            }
            self.rows = rows;
        }

        // Drinks cannot be added through a party that has ended.
        this.hasEnded = function () {
            var party = self.store.party.data;
            return !!party && !!party.endTime && self.now >= new Date(party.endTime).getTime();
        };

        this.$doCheck = function () {
            var party = self.store.party.data;
            self.start = party && party.endTime ? PartyTime.at(party.startTime) : null;
            self.end = party && party.endTime ? PartyTime.at(party.endTime) : null;
            var participants = self.store.participants.data;
            if (participants !== shownParticipants) {
                shownParticipants = participants;
                showParticipants(participants || []);
            }
        };

        this.$onInit = function () {
            releaseParty = self.store.party.use();
            releaseParticipants = self.store.participants.use();
            refresh = $interval(function () { self.now = Date.now(); }, 60000);
        };

        this.$onDestroy = function () {
            releaseParty();
            releaseParticipants();
            $interval.cancel(refresh);
        };
    }

    PartyPageCtrl.$inject = ['$routeParams', '$interval', 'PartyStore', 'PartyTime'];

    angular.module('ryyppy').component('partyPage', {
        controller: PartyPageCtrl,
        template: `
    <party-menu active="party" party="$ctrl.store.party.data"></party-menu>

    <div class="party-time" ng-if="$ctrl.start"><span>{{ $ctrl.start }}</span> – <span>{{ $ctrl.end }}</span></div>

    <div class="container-fluid" ng-repeat="row in $ctrl.rows track by row[0].id">
        <div class="row-fluid">
            <div class="span4" ng-repeat="participant in row track by participant.id">
                <drinker participant="participant" party-history="true" ended="$ctrl.hasEnded()"></drinker>
            </div>
        </div>
    </div>`
    });
})(angular);
