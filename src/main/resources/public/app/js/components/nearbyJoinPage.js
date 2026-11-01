(function (angular) {
    'use strict';

    /**
     * Confirms joining a party found nearby. The position is fetched for the preview and again for
     * the join, sent with that request and kept nowhere else: not in scope, a store or browser storage.
     */
    function NearbyJoinPageCtrl($routeParams, $location, RyyppyAPI, PartiesStore, Geolocation, Notify, PartyTime) {
        var self = this;
        var partyId = $routeParams.partyId;
        var attempt = 0;
        var destroyed = false;

        // 'locating', 'preview', 'invalid', 'denied' or 'unavailable'
        this.state = 'locating';
        this.party = null;
        this.joining = false;

        function current(attemptId) {
            return attemptId === attempt && !destroyed;
        }

        function openParty(id) {
            $location.path('/party/' + id).replace();
        }

        this.startText = function () {
            return PartyTime.at(self.party.startTime);
        };

        this.endText = function () {
            return PartyTime.at(self.party.endTime);
        };

        this.load = function () {
            var attemptId = ++attempt;
            self.state = 'locating';
            self.joining = false;
            Geolocation.current().then(function (position) {
                return RyyppyAPI.getNearbyPartyPreview(partyId, position.latitude, position.longitude).then(function (preview) {
                    if (!current(attemptId)) {
                        return;
                    }
                    if (preview.alreadyMember) {
                        Notify.success("Olet jo mukana näissä bileissä", preview.name);
                        openParty(preview.partyId);
                        return;
                    }
                    self.party = preview;
                    self.state = 'preview';
                }, function () {
                    if (current(attemptId)) {
                        self.state = 'invalid';
                    }
                });
            }, function (failure) {
                if (current(attemptId)) {
                    self.state = failure.reason;
                }
            });
        };

        this.join = function () {
            var attemptId = ++attempt;
            self.joining = true;
            Geolocation.current().then(function (position) {
                return PartiesStore.joinNearby(partyId, position.latitude, position.longitude).then(function (party) {
                    Notify.success("Tervetuloa bileisiin!", "Liityit bileisiin " + party.name + ".");
                    if (current(attemptId)) {
                        openParty(party.id);
                    }
                }, function (response) {
                    if (!current(attemptId)) {
                        return;
                    }
                    self.joining = false;
                    if (response && response.status === 404) {
                        self.state = 'invalid';
                    }
                });
            }, function (failure) {
                if (current(attemptId)) {
                    self.joining = false;
                    self.state = failure.reason;
                }
            });
        };

        this.$onInit = function () {
            self.load();
        };

        this.$onDestroy = function () {
            destroyed = true;
        };
    }

    NearbyJoinPageCtrl.$inject = ['$routeParams', '$location', 'RyyppyAPI', 'PartiesStore', 'Geolocation', 'Notify', 'PartyTime'];

    var LOCATE_ICON = '<svg class="nearby-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="6.5"></circle><circle cx="12" cy="12" r="2" fill="currentColor"></circle><path d="M12 2v3.5M12 18.5V22M2 12h3.5M18.5 12H22"></path></svg>';

    angular.module('ryyppy').component('nearbyJoinPage', {
        controller: NearbyJoinPageCtrl,
        template: `
    <user-menu></user-menu>

    <div class="container-fluid">
        <div class="row-fluid">
            <div class="span10 offset1 join-page nearby-page nearby-join-page">
                <p ng-if="$ctrl.state === 'locating'" role="status"><span class="location-invite-spinner"></span> Haetaan sijaintia…</p>

                <div ng-if="$ctrl.state === 'preview'">
                    <p class="join-page-lead">Bileet lähelläsi</p>
                    <div class="party color1 join-page-party">
                        <h1>{{ $ctrl.party.name }}</h1>
                        <p>Alkoi {{ $ctrl.startText() }}</p>
                        <p>Päättyy {{ $ctrl.endText() }}</p>
                        <div class="nearby-party-distance">
                            <svg class="nearby-icon" aria-hidden="true" viewBox="0 0 24 24"><path d="M12 22s7-7.2 7-12.5A7 7 0 0 0 5 9.5C5 14.8 12 22 12 22z" fill="currentColor"></path><circle cx="12" cy="9.5" r="2.6" fill="#fff"></circle></svg>
                            <span>{{ $ctrl.party.distance | roughDistance }}</span>
                        </div>
                    </div>
                    <p class="join-link-note">Liittymällä bilettäjät näkevät lukemasi bileiden ajalta, ja sinä näet heidän lukemansa bileiden alusta.</p>
                    <div class="join-page-actions">
                        <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.join()" ng-disabled="$ctrl.joining">Liity bileisiin</button>
                        <a class="btn btn-large" href="#!/nearby">Peruuta</a>
                    </div>
                </div>

                <div class="join-page-invalid" ng-if="$ctrl.state === 'invalid'">
                    <svg role="img" aria-label="Ei voi liittyä" width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><circle cx="9" cy="12" r="6.5" stroke-dasharray="2.5 2.5"></circle><circle cx="9" cy="12" r="1.4" fill="currentColor"></circle><path d="M19.5 9.5a2.5 2.5 0 0 0-5 0c0 2 2.5 4.5 2.5 4.5s2.5-2.5 2.5-4.5z"></path></svg>
                    <h1>Bileisiin ei voi liittyä</h1>
                    <p>Bileet eivät ole enää avoinna sijainnilla tai olet alueen ulkopuolella. Pyydä kutsulinkki bileisiin osallistuvalta.</p>
                    <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.load()">${LOCATE_ICON}Päivitä sijainti</button>
                    <a class="btn btn-large" href="#!/nearby">Takaisin listaan</a>
                </div>

                <location-problem ng-if="$ctrl.state === 'denied' || $ctrl.state === 'unavailable'"
                                  reason="$ctrl.state" on-retry="$ctrl.load()"></location-problem>
            </div>
        </div>
    </div>`
    });
})(angular);
