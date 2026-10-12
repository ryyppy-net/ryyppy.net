(function (angular) {
    'use strict';

    function UserPageCtrl($element, $timeout, InitialData, Poller, RyyppyAPI, UserHistoryGraph) {
        var self = this;
        var stopPolling = null;
        var graph = null;
        var graphTimeout = null;

        function applyProfile(data) {
            data.type = 'profile';
            data.color = 1;
            self.participants = [data];

            graphTimeout = $timeout(function () {
                if (!graph) {
                    var container = $element[0].querySelector('#historyGraph');
                    graph = new UserHistoryGraph(self.participants[0], container, RyyppyAPI, InitialData);
                }
                graph.update();
            }, 0);
        }

        this.refreshProfile = function () {
            // The server embeds the profile into the page on load (see
            // templates/app/index.html) so the very first refresh can skip the XHR.
            var initialProfile = InitialData.take('PROFILE');
            if (initialProfile) {
                applyProfile(initialProfile);
                return;
            }
            RyyppyAPI.getProfile().then(applyProfile);
        };

        this.refreshParties = function () {
            var initialParties = InitialData.take('PARTIES');
            if (initialParties) {
                self.parties = initialParties;
                return;
            }
            RyyppyAPI.getParties().then(function (data) {
                self.parties = data;
            });
        };

        this.refreshOwnDrinks = function () {
            var initialDrinks = InitialData.take('DRINKS');
            if (initialDrinks) {
                self.drinks = initialDrinks;
                return;
            }
            RyyppyAPI.getOwnDrinks().then(function (data) {
                self.drinks = data;
            });
        };

        this.startPolling = function () {
            stopPolling = Poller.start(function () {
                self.refreshProfile();
                self.refreshOwnDrinks();
            }, 60000);
        };

        this.endPolling = function () {
            if (stopPolling) {
                stopPolling();
                stopPolling = null;
            }
        };

        this.partySort = function (party) {
            return new Date(party.startTime).getTime();
        };

        this.drinkSort = function (drink) {
            return new Date(drink.timestamp).getTime();
        };

        this.drinksChanged = function () {
            self.endPolling();
            self.startPolling();
        };

        this.removeDrink = function (drink) {
            RyyppyAPI.deleteDrink({type: 'profile'}, drink.id).then(function () {
                self.endPolling();
                self.startPolling();
            });
        };

        this.$onInit = function () {
            self.refreshParties();
            self.startPolling();
        };

        this.$onDestroy = function () {
            self.endPolling();
            $timeout.cancel(graphTimeout);
            if (graph) {
                graph.destroy();
            }
        };
    }

    UserPageCtrl.$inject = ['$element', '$timeout', 'InitialData', 'Poller', 'RyyppyAPI', 'UserHistoryGraph'];

    angular.module('ryyppy').component('userPage', {
        controller: UserPageCtrl,
        template: `
    <user-menu active="user"></user-menu>

    <div class="container-fluid">
        <div class="row-fluid">
            <div class="span10 offset1">
                <drinker ng-repeat="participant in $ctrl.participants track by participant.id" participant="participant" on-drinks-changed="$ctrl.drinksChanged()"></drinker>

                <h2>Bileesi</h2>

                <div ng-show="$ctrl.parties.length <= 0">
                    <p>
                        Et näytä olevan vielä missään bileissä. Voit lisätä uudet bileet
                        <a href="#!/party-admin">bileiden hallinnassa</a> tai antaa rekisteröityessäsi käyttämäsi
                        sähköpostiosoitteen kavereillesi, jotta he voivat kutsua sinut bileisiinsä.
                    </p>
                </div>
                <div ng-show="$ctrl.parties.length > 0">
                    <p>
                        Jos haluat muokata bileitäsi, voit tehdä sen <a href="#!/party-admin">bileiden hallinnassa</a>.
                    </p>

                    <div ng-repeat="party in $ctrl.parties | orderBy:$ctrl.partySort:true track by party.id">
                        <a href="#!/party/{{ ::party.id }}">
                            <div class="party color{{ ($index % 12) + 1 }}">
                                <div>{{ ::party.name }} <small style="float: right;"><span class="hidden-phone">Alkamisaika:</span> {{ ::party.startTime | formatDateTime }}</small></div>
                                <ul class="inline" ng-show="party.participants.length > 0" style="margin-top: 5px; margin-bottom: 0px;">
                                    <li ng-repeat="participantPreview in party.participants"><img ng-src="{{ participantPreview.profilePictureUrl }}&size=30" title="{{ participantPreview.name }}" /></li>
                                </ul>
                            </div>
                        </a>
                    </div>
                </div>

                <h2>Historia</h2>

                <div class="historyGraphContainer">
                    <div id="historyGraph">
                        <canvas></canvas>
                    </div>
                </div>

                <h2>5 viimeisintä juomaa</h2>

                <div class="row-fluid" ng-repeat="drink in $ctrl.drinks | orderBy:$ctrl.drinkSort:true | limitTo:5 track by drink.id" style="margin-top: 10px;">
                    <div class="span11">{{ drink.timestamp | formatISODateTime }} (Annoksia: {{ drink.amountOfShots | roundAmountOfShots }})</div>
                    <div class="span1"><button ng-click="$ctrl.removeDrink(drink)" class="btn btn-danger">Poista</button></div>
                </div>
            </div>
        </div>
    </div>`
    });
})(angular);
