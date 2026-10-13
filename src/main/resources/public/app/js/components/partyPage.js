(function (angular) {
    'use strict';

    function PartyPageCtrl($routeParams, PartyStore) {
        var self = this;
        var releaseParty = null;
        var releaseParticipants = null;
        var shownParticipants = null;

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

        this.$doCheck = function () {
            var participants = self.store.participants.data;
            if (participants !== shownParticipants) {
                shownParticipants = participants;
                showParticipants(participants || []);
            }
        };

        this.$onInit = function () {
            releaseParty = self.store.party.use();
            releaseParticipants = self.store.participants.use();
        };

        this.$onDestroy = function () {
            releaseParty();
            releaseParticipants();
        };
    }

    PartyPageCtrl.$inject = ['$routeParams', 'PartyStore'];

    angular.module('ryyppy').component('partyPage', {
        controller: PartyPageCtrl,
        template: `
    <party-menu active="party" party="$ctrl.store.party.data"></party-menu>

    <div class="container-fluid" ng-repeat="row in $ctrl.rows track by row[0].id">
        <div class="row-fluid">
            <div class="span4" ng-repeat="participant in row track by participant.id">
                <drinker participant="participant"></drinker>
            </div>
        </div>
    </div>`
    });
})(angular);
