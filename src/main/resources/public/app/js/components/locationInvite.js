(function (angular) {
    'use strict';

    // The radii the slider offers: 100 m steps up to 1 km, then 500 m steps up to the 5 km the API allows.
    var RADII = [100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000];
    var DEFAULT_RADIUS = 1000;

    function radiusIndex(radiusM) {
        var nearest = 0;
        RADII.forEach(function (radius, index) {
            if (Math.abs(radius - radiusM) < Math.abs(RADII[nearest] - radiusM)) {
                nearest = index;
            }
        });
        return nearest;
    }

    // "700 m", "1 km", "1,5 km"
    function radiusText(radiusM) {
        return radiusM < 1000 ? radiusM + ' m' : String(radiusM / 1000).replace('.', ',') + ' km';
    }

    /**
     * The party's location invite on its Bilettäjät page. The place is always the member's
     * current position from the browser; the invite lasts until the party ends.
     */
    function LocationInviteCtrl($window, PartyStore, Geolocation, Notify, PartyTime) {
        var self = this;
        var release = null;

        this.radii = RADII;
        this.radiusText = radiusText;
        this.host = $window.location.hostname;

        this.invite = function () {
            return self.store.locationInvite.data;
        };

        this.ended = function () {
            var party = self.store.party.data;
            return !!party && PartyTime.status(party.startTime, party.endTime, Date.now()).state === 'ended';
        };

        this.endText = function () {
            return PartyTime.at(self.store.party.data.endTime);
        };

        this.setAtText = function () {
            return PartyTime.at(self.invite().createdAt);
        };

        this.draftRadius = function () {
            return RADII[self.draft.radiusIndex];
        };

        // The area's circle in the diagram grows with the slider.
        this.draftCircleRadius = function () {
            return 22 + self.draft.radiusIndex * 46 / (RADII.length - 1);
        };

        this.oneKmPosition = (radiusIndex(1000) / (RADII.length - 1) * 100) + '%';

        function locate() {
            self.mode = 'locating';
            Geolocation.current().then(function (position) {
                var radius = self.draft ? self.draft.radiusIndex : radiusIndex(DEFAULT_RADIUS);
                self.draft = {
                    latitude: position.latitude,
                    longitude: position.longitude,
                    radiusIndex: radius,
                    accuracy: Math.round(position.accuracy),
                    fetchedAt: PartyTime.clock(position.at),
                    fresh: true
                };
                self.mode = 'setting';
            }, function (failure) {
                self.failure = failure.reason;
                self.mode = 'failed';
            });
        }

        this.turnOn = function () {
            self.draft = null;
            locate();
        };

        this.change = function () {
            var invite = self.invite();
            self.draft = {
                latitude: invite.latitude,
                longitude: invite.longitude,
                radiusIndex: radiusIndex(invite.radiusM),
                fresh: false
            };
            self.mode = 'setting';
        };

        this.relocate = locate;

        this.cancel = function () {
            self.draft = null;
            self.mode = 'view';
        };

        this.save = function () {
            var wasOn = !!self.invite();
            var radiusM = self.draftRadius();
            self.busy = true;
            PartyStore.setLocationInvite(self.partyId, {
                latitude: self.draft.latitude,
                longitude: self.draft.longitude,
                radiusM: radiusM
            }).then(function () {
                self.cancel();
                if (wasOn) {
                    Notify.success("Sijaintikutsu päivitetty!", "Alue: " + radiusText(radiusM) + ".");
                } else {
                    Notify.success("Sijaintikutsu päällä!", "Lähellä olevat löytävät nyt bileet.");
                }
            }).finally(function () {
                self.busy = false;
            });
        };

        this.turnOff = function () {
            self.busy = true;
            PartyStore.revokeLocationInvite(self.partyId).finally(function () {
                self.busy = false;
            });
        };

        this.$onInit = function () {
            self.store = PartyStore.get(self.partyId);
            self.loaded = false;
            self.busy = false;
            self.mode = 'view';
            self.draft = null;
            self.failure = null;
            release = self.store.locationInvite.use();
            self.store.locationInvite.load().then(function () {
                self.loaded = true;
            });
        };

        this.$onDestroy = function () {
            if (release) {
                release();
            }
        };
    }

    LocationInviteCtrl.$inject = ['$window', 'PartyStore', 'Geolocation', 'Notify', 'PartyTime'];

    angular.module('ryyppy').component('locationInvite', {
        bindings: {
            partyId: '<'
        },
        controller: LocationInviteCtrl,
        template: `
    <section class="location-invite" ng-if="$ctrl.loaded && $ctrl.store.party.data">
        <h1>Sijaintikutsu</h1>

        <div ng-if="$ctrl.ended()">
            <p ng-if="$ctrl.invite()"><span class="location-invite-status location-invite-status-off"><span class="location-invite-dot"></span>Päättynyt</span></p>
            <p>Bileet päättyivät {{ $ctrl.endText() }}, joten niihin ei voi enää liittyä.</p>
        </div>

        <div ng-if="!$ctrl.ended() && $ctrl.mode === 'view' && !$ctrl.invite()">
            <p>Anna lähellä olevien löytää bileet ja liittyä ilman linkkiä. Kirjautunut käyttäjä, joka on valitsemallasi alueella, näkee bileet sovelluksessa.</p>
            <div class="join-link-actions">
                <button type="button" class="btn btn-success" ng-click="$ctrl.turnOn()">Ota sijaintikutsu käyttöön</button>
            </div>
        </div>

        <div ng-if="!$ctrl.ended() && $ctrl.mode === 'view' && $ctrl.invite()">
            <p><span class="location-invite-status"><span class="location-invite-dot"></span>Päällä</span></p>
            <dl class="location-invite-facts">
                <dt>Alue</dt><dd>{{ $ctrl.radiusText($ctrl.invite().radiusM) }}</dd>
                <dt>Paikka</dt><dd>Bilettäjän sijainti {{ $ctrl.setAtText() }}</dd>
                <dt>Voimassa</dt><dd>{{ $ctrl.endText() }} asti</dd>
            </dl>
            <div class="join-link-actions">
                <button type="button" class="btn" ng-click="$ctrl.change()" ng-disabled="$ctrl.busy">Muuta</button>
                <button type="button" class="btn btn-danger" ng-click="$ctrl.turnOff()" ng-disabled="$ctrl.busy">Poista käytöstä</button>
            </div>
        </div>

        <p ng-if="!$ctrl.ended() && $ctrl.mode === 'locating'" role="status"><span class="location-invite-spinner"></span> Haetaan sijaintia…</p>

        <div ng-if="!$ctrl.ended() && $ctrl.mode === 'failed'">
            <div class="location-invite-error" role="alert">
                <p><strong>Sijaintia ei saatu.</strong></p>
                <p ng-if="$ctrl.failure === 'denied'">Selain on estänyt sijainnin {{ $ctrl.host }}-sivustolta. Salli sijainti selaimen sivustoasetuksista ja yritä uudelleen.</p>
                <p ng-if="$ctrl.failure !== 'denied'">Laite ei pystynyt selvittämään sijaintiasi. Tarkista, että sijaintipalvelut ovat päällä, ja yritä uudelleen.</p>
            </div>
            <div class="join-link-actions">
                <button type="button" class="btn" ng-click="$ctrl.relocate()">Yritä uudelleen</button>
                <button type="button" class="btn" ng-click="$ctrl.cancel()">Peruuta</button>
            </div>
        </div>

        <form ng-if="!$ctrl.ended() && $ctrl.mode === 'setting'" ng-submit="$ctrl.save()">
            <span class="location-invite-label">Bileiden paikka</span>
            <div class="location-invite-diagram">
                <svg viewBox="0 0 350 150" role="img" aria-label="Bileiden paikka ja alue">
                    <circle cx="175" cy="75" ng-attr-r="{{ $ctrl.draftCircleRadius() }}" fill="rgba(98,196,98,.18)" stroke="#62c462" stroke-width="2.5" stroke-dasharray="7 5"></circle>
                    <path ng-attr-d="M175 75h{{ $ctrl.draftCircleRadius() }}" stroke="#62c462" stroke-width="2"></path>
                    <text ng-attr-x="{{ 175 + $ctrl.draftCircleRadius() / 2 }}" y="68" fill="#e2e2e2" font-size="15" text-anchor="middle">{{ $ctrl.radiusText($ctrl.draftRadius()) }}</text>
                    <circle cx="175" cy="75" r="11" fill="rgba(26,95,180,.35)"></circle>
                    <circle cx="175" cy="75" r="5.5" fill="#4a90e2" stroke="#fff" stroke-width="2"></circle>
                </svg>
            </div>
            <p class="location-invite-small" ng-if="$ctrl.draft.fresh">Sijaintisi nyt, tarkkuus ±{{ $ctrl.draft.accuracy }} m. Haettu klo {{ $ctrl.draft.fetchedAt }}.</p>
            <p class="location-invite-small" ng-if="!$ctrl.draft.fresh">Bilettäjän sijainti {{ $ctrl.setAtText() }}.</p>
            <div class="join-link-actions">
                <button type="button" class="btn" ng-click="$ctrl.relocate()">{{ $ctrl.draft.fresh ? 'Hae sijainti uudelleen' : 'Käytä nykyistä sijaintiani' }}</button>
            </div>

            <label class="location-invite-label" for="locationInviteRadius">Alue: {{ $ctrl.radiusText($ctrl.draftRadius()) }}</label>
            <input id="locationInviteRadius" class="location-invite-radius" type="range" min="0" max="{{ ::$ctrl.radii.length - 1 }}" step="1"
                   ng-model="$ctrl.draft.radiusIndex" aria-valuetext="{{ $ctrl.radiusText($ctrl.draftRadius()) }}" />
            <div class="location-invite-scale" aria-hidden="true">
                <span>100 m</span><span class="location-invite-scale-mark" ng-style="{left: $ctrl.oneKmPosition}">1 km</span><span>5 km</span>
            </div>

            <p class="location-invite-small">Voimassa bileiden loppuun, {{ $ctrl.endText() }} asti.</p>
            <p class="join-link-note">Alueella oleva voi liittyä ja näkee kaikkien lukemat bileiden alusta. Ulkopuoliset näkevät vain bileiden nimen ja ajat, ja kuinka kaukana bileet ovat. Tarkkaa paikkaa ei näytetä.</p>
            <div class="join-link-actions">
                <button type="button" class="btn" ng-click="$ctrl.cancel()">Peruuta</button>
                <button type="submit" class="btn btn-success" ng-disabled="$ctrl.busy">{{ $ctrl.invite() ? 'Tallenna' : 'Ota käyttöön' }}</button>
            </div>
        </form>
    </section>`
    });
})(angular);
