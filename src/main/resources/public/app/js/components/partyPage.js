'use strict';

function PartyPageCtrl($routeParams, Poller, RyyppyAPI) {
    var self = this;
    var stopPolling = null;

    this.refreshParty = function () {
        RyyppyAPI.getParty($routeParams.partyId).then(function (data) {
            self.party = data;
        });
    };

    this.refreshParticipants = function () {
        RyyppyAPI.getPartyParticipants($routeParams.partyId).then(function (data) {
            // Should this be added in backend?
            for (var i = 0; i < data.length; i++) {
                data[i].type = 'participant';
                data[i].partyId = $routeParams.partyId;
                data[i].color = (i % 12) + 1;
            }

            var rowsAmount = Math.ceil(data.length / 3);
            var rows = new Array(rowsAmount);
            for (var i = 0; i < rowsAmount; i++) {
                var colsAmount = Math.min(3, data.length - i * 3);
                var cols = new Array(colsAmount);
                rows[i] = cols;
                for (var j = 0; j < cols.length; j++) {
                    cols[j] = data[i * 3 + j];
                }
            }
            self.rows = rows;
        });
    };

    this.startPolling = function () {
        stopPolling = Poller.start(self.refreshParticipants, 60000);
    };

    this.endPolling = function () {
        if (stopPolling) {
            stopPolling();
            stopPolling = null;
        }
    };

    this.drinksChanged = function () {
        self.endPolling();
        self.startPolling();
    };

    this.$onInit = function () {
        self.refreshParty();
        self.startPolling();
    };

    this.$onDestroy = function () {
        self.endPolling();
    };
}

PartyPageCtrl.$inject = ['$routeParams', 'Poller', 'RyyppyAPI'];

angular.module('ryyppy').component('partyPage', {
    controller: PartyPageCtrl,
    template: `
<party-menu active="party" party="$ctrl.party"></party-menu>

<div class="container-fluid" ng-repeat="row in $ctrl.rows track by row[0].id">
    <div class="row-fluid">
        <div class="span4" ng-repeat="participant in row track by participant.id">
            <drinker participant="participant" on-drinks-changed="$ctrl.drinksChanged()"></drinker>
        </div>
    </div>
</div>`
});
