(function (angular) {
    'use strict';

    function PartyPageCtrl($routeParams, $interval, PartyStore, PartyTime) {
        var self = this;
        var releaseParty = null;
        var releaseParticipants = null;
        var shownParticipants = null;
        var refresh = null;
        var cached = null;

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

        this.$doCheck = function () {
            self.timeInfo = time();
            var participants = self.store.participants.data;
            if (participants !== shownParticipants) {
                shownParticipants = participants;
                showParticipants(participants || []);
            }
        };

        function time() {
            var party = self.store.party.data;
            if (!party || !party.endTime) {
                return null;
            }
            if (!cached || cached.startTime !== party.startTime || cached.endTime !== party.endTime || cached.now !== self.now) {
                cached = {
                    startTime: party.startTime,
                    endTime: party.endTime,
                    now: self.now,
                    status: PartyTime.status(party.startTime, party.endTime, self.now),
                    range: PartyTime.range(party.startTime, party.endTime, true),
                    shortRange: PartyTime.range(party.startTime, party.endTime),
                    dayScale: PartyTime.dayScale(party.startTime, party.endTime)
                };
            }
            return cached;
        }

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

    <div class="time-strip" ng-if="$ctrl.timeInfo" ng-class="'time-strip-' + $ctrl.timeInfo.status.state">
        <div class="hidden-phone">
            <div class="time-strip-row">
                <span class="time-strip-main">
                    <span class="status-badge status-badge-{{ $ctrl.timeInfo.status.state }}">{{ $ctrl.timeInfo.status.state == 'ended' ? 'PÄÄTTYNYT' : ($ctrl.timeInfo.status.state == 'lastHour' ? 'VIIMEINEN TUNTI' : 'KÄYNNISSÄ') }}</span>
                    {{ $ctrl.timeInfo.range }}
                </span>
                <span class="time-strip-left">
                    <span ng-if="$ctrl.timeInfo.status.state == 'running'">{{ $ctrl.timeInfo.status.elapsedText }} · {{ $ctrl.timeInfo.status.leftText }}</span>
                    <strong class="time-strip-warning" ng-if="$ctrl.timeInfo.status.state == 'lastHour'">{{ $ctrl.timeInfo.status.leftText }}</strong>
                    <span ng-if="$ctrl.timeInfo.status.state == 'ended'">{{ $ctrl.timeInfo.status.lengthText }}</span>
                </span>
            </div>
            <div class="time-strip-bar" ng-if="$ctrl.timeInfo.status.state != 'ended'"><div class="time-strip-bar-fill" ng-style="{width: ($ctrl.timeInfo.status.progress * 100) + '%'}"></div></div>
            <div class="time-strip-scale"><span ng-repeat="label in $ctrl.timeInfo.dayScale track by $index">{{ label }}</span></div>
        </div>
        <div class="visible-phone">
            <div class="time-strip-row">
                <span class="status-badge status-badge-{{ $ctrl.timeInfo.status.state }}">{{ $ctrl.timeInfo.status.state == 'ended' ? 'PÄÄTTYNYT' : ($ctrl.timeInfo.status.state == 'lastHour' ? 'VIIMEINEN TUNTI' : 'KÄYNNISSÄ') }}</span>
                <span class="time-strip-left">
                    <span ng-if="$ctrl.timeInfo.status.state == 'running'">{{ $ctrl.timeInfo.status.leftText }}</span>
                    <strong class="time-strip-warning" ng-if="$ctrl.timeInfo.status.state == 'lastHour'">{{ $ctrl.timeInfo.status.leftText }}</strong>
                    <span ng-if="$ctrl.timeInfo.status.state == 'ended'">{{ $ctrl.timeInfo.status.lengthText }}</span>
                </span>
            </div>
            <div>{{ $ctrl.timeInfo.shortRange }}</div>
            <div class="time-strip-bar" ng-if="$ctrl.timeInfo.status.state != 'ended'"><div class="time-strip-bar-fill" ng-style="{width: ($ctrl.timeInfo.status.progress * 100) + '%'}"></div></div>
        </div>
    </div>

    <div class="container-fluid" ng-repeat="row in $ctrl.rows track by row[0].id">
        <div class="row-fluid">
            <div class="span4" ng-repeat="participant in row track by participant.id">
                <drinker participant="participant" party="$ctrl.store.party.data"></drinker>
            </div>
        </div>
    </div>`
    });
})(angular);
