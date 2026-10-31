(function (angular) {
    'use strict';

    function UserPageCtrl($interval, Drinks, OwnDrinksStore, PartiesStore, ProfileStore, PartyTime) {
        var self = this;
        var releases = [];
        var refresh = null;
        var shownProfile = null;

        this.parties = PartiesStore.parties;
        this.partiesShown = PartiesStore.shown;
        this.hasMoreParties = PartiesStore.hasMore;
        this.showMoreParties = PartiesStore.showMore;
        this.drinks = OwnDrinksStore.drinks;
        this.participants = [];
        this.now = Date.now();

        this.$doCheck = function () {
            var profile = ProfileStore.profile.data;
            if (profile !== shownProfile) {
                shownProfile = profile;
                self.participants = profile ? [angular.extend({type: 'profile', color: 1}, profile)] : [];
            }
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

        this.drinkSort = function (drink) {
            return new Date(drink.timestamp).getTime();
        };

        this.removeDrink = function (drink) {
            Drinks.remove({type: 'profile'}, drink.id);
        };

        this.$onInit = function () {
            releases = [
                ProfileStore.profile.use(),
                OwnDrinksStore.drinks.use(),
                PartiesStore.parties.use()
            ];
            refresh = $interval(function () { self.now = Date.now(); }, 60000);
        };

        this.$onDestroy = function () {
            $interval.cancel(refresh);
            releases.forEach(function (release) {
                release();
            });
        };
    }

    UserPageCtrl.$inject = ['$interval', 'Drinks', 'OwnDrinksStore', 'PartiesStore', 'ProfileStore', 'PartyTime'];

    angular.module('ryyppy').component('userPage', {
        controller: UserPageCtrl,
        template: `
    <user-menu active="user"></user-menu>

    <div class="container-fluid">
        <div class="row-fluid">
            <div class="span10 offset1">
                <drinker ng-repeat="participant in $ctrl.participants track by participant.id" participant="participant"></drinker>

                <h2>Bileesi</h2>

                <div ng-if="$ctrl.parties.data.length <= 0">
                    <p>
                        Et näytä olevan vielä missään bileissä. Voit lisätä uudet bileet
                        <a href="#!/party-admin">bileiden hallinnassa</a> tai antaa rekisteröityessäsi käyttämäsi
                        sähköpostiosoitteen kavereillesi, jotta he voivat kutsua sinut bileisiinsä.
                    </p>
                </div>
                <div ng-if="$ctrl.parties.data.length > 0">
                    <p>
                        Jos haluat muokata bileitäsi, voit tehdä sen <a href="#!/party-admin">bileiden hallinnassa</a>.
                    </p>

                    <div ng-repeat="party in $ctrl.parties.data | orderBy:[$ctrl.partySort, 'id']:true | limitTo:$ctrl.partiesShown() track by party.id">
                        <a href="#!/party/{{ ::party.id }}">
                            <div class="party color{{ ($index % 12) + 1 }}">
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
                        </a>
                    </div>
                    <button id="morePartiesButton" class="btn" ng-if="$ctrl.hasMoreParties()" ng-click="$ctrl.showMoreParties()">Näytä lisää bileitä</button>
                </div>

                <h2>Historia</h2>

                <drink-history-graph drinks="$ctrl.drinks.data"></drink-history-graph>

                <h2>5 viimeisintä juomaa</h2>

                <div class="drink-slot" ng-repeat="drink in $ctrl.drinks.data | orderBy:$ctrl.drinkSort:true | limitTo:5 track by drink.id">
                    <div class="drink-item">
                        <div class="row-fluid" style="margin-top: 10px;">
                            <div class="span11">{{ drink.timestamp | formatISODateTime }} (Annoksia: {{ drink.amountOfShots | roundAmountOfShots }})</div>
                            <div class="span1"><button ng-click="$ctrl.removeDrink(drink)" class="btn btn-danger">Poista</button></div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>`
    });
})(angular);
