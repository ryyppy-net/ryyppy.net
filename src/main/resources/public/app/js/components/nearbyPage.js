(function (angular) {
    'use strict';

    /**
     * Parties the signed-in user can join from where they are now. The position goes into the
     * search request and nowhere else: not into scope, a store or browser storage.
     */
    function NearbyPageCtrl(RyyppyAPI, Geolocation, PartyTime) {
        var self = this;
        var search = 0;
        var destroyed = false;


        // 'locating', 'list', 'empty', 'denied', 'unavailable' or 'failed'
        this.state = 'locating';
        this.parties = [];
        this.updatedAt = null;

        this.startText = function (party) {
            return PartyTime.at(party.startTime);
        };

        this.endText = function (party) {
            return PartyTime.at(party.endTime);
        };

        this.progress = function (party) {
            return PartyTime.status(party.startTime, party.endTime, Date.now()).progress;
        };

        this.find = function () {
            var current = ++search;
            self.state = 'locating';
            Geolocation.current().then(function (position) {
                return RyyppyAPI.findNearbyParties(position.latitude, position.longitude).then(function (parties) {
                    if (current !== search || destroyed) {
                        return;
                    }
                    self.parties = parties;
                    self.updatedAt = PartyTime.clock(Date.now());
                    self.state = parties.length > 0 ? 'list' : 'empty';
                }, function () {
                    if (current === search && !destroyed) {
                        self.state = 'failed';
                    }
                });
            }, function (failure) {
                if (current === search && !destroyed) {
                    self.state = failure.reason;
                }
            });
        };

        this.$onInit = function () {
            self.find();
        };

        this.$onDestroy = function () {
            destroyed = true;
        };
    }

    NearbyPageCtrl.$inject = ['RyyppyAPI', 'Geolocation', 'PartyTime'];

    var LOCATE_ICON = '<svg class="nearby-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="6.5"></circle><circle cx="12" cy="12" r="2" fill="currentColor"></circle><path d="M12 2v3.5M12 18.5V22M2 12h3.5M18.5 12H22"></path></svg>';

    angular.module('ryyppy').component('nearbyPage', {
        controller: NearbyPageCtrl,
        template: `
    <user-menu></user-menu>

    <div class="container-fluid">
        <div class="row-fluid">
            <div class="span10 offset1 nearby-page">
                <div ng-if="$ctrl.state === 'locating'">
                    <h1>Bileet lähellä</h1>
                    <p role="status"><span class="location-invite-spinner"></span> Haetaan sijaintia…</p>
                    <p class="join-link-note location-invite-small">Sijaintiasi käytetään vain bileiden etsimiseen. Bilettäjät eivät näe sitä.</p>
                </div>

                <div ng-if="$ctrl.state === 'list'">
                    <h1>Bileet lähellä</h1>
                    <p class="nearby-page-muted location-invite-small">Bileet, joiden alueella olet nyt. Päivitetty klo {{ $ctrl.updatedAt }}.</p>
                    <a class="nearby-party" ng-repeat="party in $ctrl.parties track by party.partyId" ng-href="#!/nearby/{{ party.partyId }}">
                        <div class="party color{{ ($index % 12) + 1 }}">
                            <div class="party-header">
                                <span class="party-header-title">{{ party.name }}</span>
                                <small class="nearby-party-times">Alkoi {{ $ctrl.startText(party) }} · päättyy {{ $ctrl.endText(party) }}</small>
                            </div>
                            <div class="nearby-party-distance">
                                <svg class="nearby-icon" aria-hidden="true" viewBox="0 0 24 24"><path d="M12 22s7-7.2 7-12.5A7 7 0 0 0 5 9.5C5 14.8 12 22 12 22z" fill="currentColor"></path><circle cx="12" cy="9.5" r="2.6" fill="#fff"></circle></svg>
                                <span>{{ party.distance | roughDistance }}</span>
                            </div>
                            <div class="party-progress"><div class="party-progress-fill" ng-style="{width: ($ctrl.progress(party) * 100) + '%'}"></div></div>
                        </div>
                    </a>
                    <div class="join-link-actions nearby-page-refresh">
                        <button type="button" class="btn" ng-click="$ctrl.find()">${LOCATE_ICON}Päivitä</button>
                    </div>
                    <p class="nearby-page-muted location-invite-small">Etkö löydä bileitä? Pyydä bilettäjää ottamaan sijaintikutsu käyttöön tai lähettämään kutsulinkki.</p>
                    <p class="join-link-note location-invite-small">Sijaintiasi käytetään vain bileiden etsimiseen. Bilettäjät eivät näe sitä.</p>
                </div>

                <div ng-if="$ctrl.state === 'empty'">
                    <h1>Bileet lähellä</h1>
                    <div class="nearby-page-message">
                        <svg class="nearby-page-message-icon" role="img" aria-label="Ei tuloksia" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><circle cx="10.5" cy="10.5" r="6.5"></circle><path d="m15.5 15.5 5 5"></path><path d="M8 10.5h5"></path></svg>
                        <h2>Lähellä ei ole bileitä</h2>
                        <p>Et ole minkään bileiden alueella juuri nyt. Bilettäjä voi ottaa sijaintikutsun käyttöön Bilettäjät-sivulla tai lähettää sinulle kutsulinkin.</p>
                        <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.find()">${LOCATE_ICON}Päivitä</button>
                        <a class="btn btn-large" href="#!/">Takaisin etusivulle</a>
                    </div>
                </div>

                <location-problem ng-if="$ctrl.state === 'denied' || $ctrl.state === 'unavailable'"
                                  reason="$ctrl.state" on-retry="$ctrl.find()"></location-problem>

                <div class="nearby-page-message" ng-if="$ctrl.state === 'failed'">
                    <h1>Haku epäonnistui</h1>
                    <p>Bileitä ei saatu haettua. Yritä hetken päästä uudelleen.</p>
                    <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.find()">Yritä uudelleen</button>
                    <a class="btn btn-large" href="#!/">Takaisin etusivulle</a>
                </div>
            </div>
        </div>
    </div>`
    });
})(angular);
