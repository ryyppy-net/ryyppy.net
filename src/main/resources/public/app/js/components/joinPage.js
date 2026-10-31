(function (angular) {
    'use strict';

    function JoinPageCtrl($routeParams, $location, RyyppyAPI, PartiesStore, Notify, PartyTime) {
        var self = this;
        var token = $routeParams.token;

        // 'loading', 'preview' or 'invalid'
        this.state = 'loading';
        this.party = null;
        this.joining = false;

        function openParty(partyId) {
            $location.path('/party/' + partyId).replace();
        }

        this.startText = function () {
            return PartyTime.at(self.party.startTime);
        };

        this.endText = function () {
            return PartyTime.at(self.party.endTime);
        };

        this.join = function () {
            self.joining = true;
            PartiesStore.joinWithLink(token).then(function (party) {
                Notify.success("Tervetuloa bileisiin!", "Liityit bileisiin " + party.name + ".");
                openParty(party.id);
            }, function (response) {
                self.joining = false;
                if (response && response.status === 404) {
                    self.state = 'invalid';
                }
            });
        };

        this.$onInit = function () {
            RyyppyAPI.getJoinLinkPreview(token).then(function (preview) {
                if (preview.alreadyMember) {
                    Notify.success("Olet jo mukana näissä bileissä", preview.name);
                    openParty(preview.partyId);
                    return;
                }
                self.party = preview;
                self.state = 'preview';
            }, function () {
                self.state = 'invalid';
            });
        };
    }

    JoinPageCtrl.$inject = ['$routeParams', '$location', 'RyyppyAPI', 'PartiesStore', 'Notify', 'PartyTime'];

    angular.module('ryyppy').component('joinPage', {
        controller: JoinPageCtrl,
        template: `
    <user-menu></user-menu>

    <div class="container-fluid">
        <div class="row-fluid">
            <div class="span10 offset1 join-page">
                <p ng-if="$ctrl.state === 'loading'">Ladataan...</p>

                <div ng-if="$ctrl.state === 'preview'">
                    <p class="join-page-lead">Sinut on kutsuttu bileisiin</p>
                    <div class="party color1 join-page-party">
                        <h1>{{ $ctrl.party.name }}</h1>
                        <p>Alkoi {{ $ctrl.startText() }}</p>
                        <p>Päättyy {{ $ctrl.endText() }}</p>
                    </div>
                    <p class="join-link-note">Liittymällä bilettäjät näkevät lukemasi bileiden ajalta, ja sinä näet heidän lukemansa bileiden alusta.</p>
                    <div class="join-page-actions">
                        <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.join()" ng-disabled="$ctrl.joining">Liity bileisiin</button>
                        <a class="btn btn-large" href="#!/">Peruuta</a>
                    </div>
                </div>

                <div class="join-page-invalid" ng-if="$ctrl.state === 'invalid'">
                    <svg role="img" aria-label="Rikkinäinen linkki" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6"><path d="M10 14a4 4 0 0 0 5.7 0l1.3-1.3M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l.7-.7M13 6.3l.7-.7a4 4 0 0 1 5.7 5.7"></path><path d="M4 4l16 16"></path></svg>
                    <h1>Linkki ei kelpaa</h1>
                    <p>Linkki on vanhentunut tai poistettu käytöstä. Pyydä uusi linkki bileisiin osallistuvalta.</p>
                    <a class="btn btn-success btn-large" href="#!/">Takaisin etusivulle</a>
                </div>
            </div>
        </div>
    </div>`
    });
})(angular);
