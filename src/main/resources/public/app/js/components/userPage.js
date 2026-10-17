(function (angular) {
    'use strict';

    function UserPageCtrl($element, $timeout, Drinks, OwnDrinksStore, PartiesStore, ProfileStore, UserHistoryGraph) {
        var self = this;
        var releases = [];
        var shownProfile = null;
        var shownHistory = null;
        var graph = null;
        var graphTimeout = null;

        this.parties = PartiesStore.parties;
        this.partiesShown = PartiesStore.shown;
        this.hasMoreParties = PartiesStore.hasMore;
        this.showMoreParties = PartiesStore.showMore;
        this.drinks = OwnDrinksStore.drinks;
        this.participants = [];

        function drawHistory(csv) {
            $timeout.cancel(graphTimeout);
            graphTimeout = $timeout(function () {
                if (!graph) {
                    graph = new UserHistoryGraph($element[0].querySelector('#historyGraph'));
                }
                graph.gotData(csv);
            }, 0, false);
        }

        this.$doCheck = function () {
            var profile = ProfileStore.profile.data;
            if (profile !== shownProfile) {
                shownProfile = profile;
                self.participants = profile ? [angular.extend({type: 'profile', color: 1}, profile)] : [];
            }

            var history = OwnDrinksStore.history.data;
            if (history !== shownHistory) {
                shownHistory = history;
                if (history !== null) {
                    drawHistory(history);
                }
            }
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
                OwnDrinksStore.history.use(),
                PartiesStore.parties.use()
            ];
        };

        this.$onDestroy = function () {
            releases.forEach(function (release) {
                release();
            });
            $timeout.cancel(graphTimeout);
            if (graph) {
                graph.destroy();
            }
        };
    }

    UserPageCtrl.$inject = ['$element', '$timeout', 'Drinks', 'OwnDrinksStore', 'PartiesStore', 'ProfileStore', 'UserHistoryGraph'];

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

                    <div ng-repeat="party in $ctrl.parties.data | orderBy:$ctrl.partySort:true | limitTo:$ctrl.partiesShown() track by party.id">
                        <a href="#!/party/{{ ::party.id }}">
                            <div class="party color{{ ($index % 12) + 1 }}">
                                <div>{{ ::party.name }} <small style="float: right;"><span class="hidden-phone">Alkamisaika:</span> {{ ::party.startTime | formatDateTime }}</small></div>
                                <ul class="inline" ng-if="party.participants.length > 0" style="margin-top: 5px; margin-bottom: 0px;">
                                    <li ng-repeat="participantPreview in party.participants"><img ng-src="{{ participantPreview.profilePictureUrl }}&size=30" title="{{ participantPreview.name }}" /></li>
                                </ul>
                            </div>
                        </a>
                    </div>
                    <button id="morePartiesButton" class="btn" ng-if="$ctrl.hasMoreParties()" ng-click="$ctrl.showMoreParties()">Näytä lisää bileitä</button>
                </div>

                <h2>Historia</h2>

                <div class="historyGraphContainer">
                    <div id="historyGraph">
                        <canvas></canvas>
                    </div>
                </div>

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
