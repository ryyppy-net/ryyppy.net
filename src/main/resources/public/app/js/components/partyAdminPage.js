(function (angular) {
    'use strict';

    function PartyAdminPageCtrl($routeParams, $location, $element, $q, $window, PartyStore, ProfileStore, Notify, PartyTime) {
        var self = this;
        var partyId = $routeParams.partyId;
        var releases = [];

        this.partyId = partyId;
        this.store = PartyStore.get(partyId);

        this.days = null;

        this.savedDays = function () {
            var party = self.store.party.data;
            return party ? PartyTime.days(party.startTime, party.endTime) : null;
        };

        this.setDays = function (days) {
            self.days = days;
        };

        this.startText = function () {
            return PartyTime.at(self.store.party.data.startTime);
        };

        this.endText = function () {
            return PartyTime.at(PartyTime.after(self.store.party.data.startTime, self.days));
        };

        this.saveDuration = function () {
            PartyStore.changeDuration(partyId, self.days).then(function () {
                Notify.success("Aika päivitetty!", "Bileet kestävät nyt " + self.days + " vrk.");
            });
        };

        this.canShare = !!$window.navigator.share;

        this.joinLinkValidUntil = function () {
            return PartyTime.at(self.store.party.data.endTime);
        };

        function joinLinkRequest(request) {
            self.joinLinkBusy = true;
            return request.finally(function () {
                self.joinLinkBusy = false;
            });
        }

        this.createJoinLink = function () {
            self.confirmingNewJoinLink = false;
            joinLinkRequest(PartyStore.createJoinLink(partyId));
        };

        this.revokeJoinLink = function () {
            self.confirmingNewJoinLink = false;
            joinLinkRequest(PartyStore.revokeJoinLink(partyId));
        };

        function selectJoinLink() {
            var input = $element[0].querySelector('#joinLinkUrl');
            input.focus();
            input.select();
        }

        this.copyJoinLink = function () {
            var clipboard = $window.navigator.clipboard;
            if (!clipboard || !clipboard.writeText) {
                selectJoinLink();
                return;
            }
            $q.when(clipboard.writeText(self.store.joinLink.data.url)).then(function () {
                Notify.success("Linkki kopioitu!", "Lähetä se kavereille.");
            }, selectJoinLink);
        };

        this.shareJoinLink = function () {
            var name = self.store.party.data ? self.store.party.data.name : '';
            $window.navigator.share({
                title: name,
                text: "Liity bileisiin " + name,
                url: self.store.joinLink.data.url
            }).catch(function (error) {
                if (error.name !== 'AbortError') {
                    throw error;
                }
            });
        };

        this.addUser = function () {
            if (self.selectedUserTypeId === 2)
                self._addRegisteredUser(self.email);
            if (self.selectedUserTypeId === 3)
                self._addGuestUser(self.name, self.sex, self.weight);
        };

        this._addRegisteredUser = function (email) {
            PartyStore.addRegisteredUser(partyId, email).then(function () {
                Notify.success("Tervetuloa takaisin!", "Lisätty käyttäjä sähköpostiosoitteella " + email + ".");
            });
        };

        this._addGuestUser = function (name, sex, weight) {
            var guest = { name: name, sex: sex, weight: weight };
            PartyStore.addGuest(partyId, guest).then(function () {
                Notify.success("Tervetuloa vieraalle!", "Lisätty vieras " + self.name + ".");
            });
        };

        this.inviteUser = function (invitation) {
            self.inviting = true;
            self.invitedFriend = invitation;
            PartyStore.invite(partyId, invitation.id).then(function () {
                self.invitedFriend = undefined;
                Notify.success("Pitkästä aikaa!", "Lisätty käyttäjä " + invitation.name + ".");
                self.inviting = false;
            });
        };

        this.removeUser = function (participant) {
            var profile = ProfileStore.profile.data;
            if (profile && participant.id === profile.id) {
                PartyStore.leave(partyId, participant).then(function () {
                    Notify.success("ULOS!", "Heitettiin " + participant.name + " pihalle.");
                    // Only party members may read the party, so after leaving it there is nothing to show here.
                    $location.path('/party-admin/');
                });
                return;
            }
            PartyStore.removeParticipant(partyId, participant).then(function () {
                Notify.success("ULOS!", "Heitettiin " + participant.name + " pihalle.");
            });
        };

        this.$onInit = function () {
            self.email = null;
            self.name = null;
            self.sex = 'MALE';
            self.weight = null;
            self.inviting = false;

            self.userTypes = [
                {TypeId: 1, TypeName: 'Vanha ystävä'},
                {TypeId: 2, TypeName: 'Rekisteröitynyt käyttäjä'},
                {TypeId: 3, TypeName: 'Vieras'}
            ];
            self.selectedUserTypeId = 1;
            self.joinLinkLoaded = false;
            self.joinLinkBusy = false;
            self.confirmingNewJoinLink = false;

            ProfileStore.profile.load();
            self.store.party.load().then(function () {
                self.days = self.savedDays();
            });
            releases = [
                self.store.party.use(),
                self.store.participants.use(),
                self.store.invitations.use(),
                self.store.joinLink.use()
            ];
            self.store.joinLink.load().then(function () {
                self.joinLinkLoaded = true;
            });
        };

        this.$onDestroy = function () {
            releases.forEach(function (release) {
                release();
            });
        };
    }

    PartyAdminPageCtrl.$inject = ['$routeParams', '$location', '$element', '$q', '$window', 'PartyStore', 'ProfileStore', 'Notify', 'PartyTime'];

    angular.module('ryyppy').component('partyAdminPage', {
        controller: PartyAdminPageCtrl,
        template: `
    <party-menu active="admin" party="$ctrl.store.party.data"></party-menu>

    <div class="container-fluid">
        <div class="row-fluid">
            <div class="span10 offset1">
                <div ng-if="$ctrl.days !== null">
                    <h1>Bileiden aika</h1>

                    <form class="form-horizontal" ng-submit="$ctrl.saveDuration()">
                        <div class="control-group">
                            <label class="control-label">Alkoi</label>
                            <div class="controls controls-static">{{ $ctrl.startText() }}</div>
                        </div>

                        <div class="control-group">
                            <span class="control-label">Kesto</span>
                            <div class="controls">
                                <day-stepper days="$ctrl.days" on-change="$ctrl.setDays(days)"></day-stepper>
                                <span class="help-inline duration-summary"><span class="duration-end">päättyy {{ $ctrl.endText() }}</span></span>
                            </div>
                        </div>

                        <div class="control-group">
                            <div class="controls">
                                <button type="submit" class="btn btn-success duration-save" ng-disabled="$ctrl.days === $ctrl.savedDays()">Tallenna</button>
                            </div>
                        </div>
                    </form>
                </div>

                <section class="join-link" ng-if="$ctrl.joinLinkLoaded">
                    <h1>Kutsulinkki</h1>

                    <div ng-if="!$ctrl.store.joinLink.data">
                        <p>Luo linkki, jonka voit lähettää kavereille. Kirjautunut käyttäjä liittyy bileisiin avaamalla sen.</p>
                        <button type="button" class="btn btn-success" ng-click="$ctrl.createJoinLink()" ng-disabled="$ctrl.joinLinkBusy">Luo kutsulinkki</button>
                    </div>

                    <div ng-if="$ctrl.store.joinLink.data">
                        <label for="joinLinkUrl">Lähetä tämä linkki kavereille</label>
                        <input id="joinLinkUrl" class="join-link-url" type="text" readonly ng-value="$ctrl.store.joinLink.data.url" />
                        <div class="join-link-actions">
                            <button type="button" class="btn btn-success" ng-click="$ctrl.copyJoinLink()">Kopioi</button>
                            <button type="button" class="btn btn-primary" ng-if="$ctrl.canShare" ng-click="$ctrl.shareJoinLink()">Jaa</button>
                        </div>
                        <p class="join-link-validity" ng-if="$ctrl.store.party.data">Voimassa {{ $ctrl.joinLinkValidUntil() }} asti</p>
                        <p class="join-link-note">Linkin saanut voi liittyä ja näkee kaikkien lukemat bileiden alusta.</p>
                        <div class="join-link-actions">
                            <button type="button" class="btn" ng-click="$ctrl.confirmingNewJoinLink = true" ng-disabled="$ctrl.joinLinkBusy">Luo uusi linkki</button>
                            <button type="button" class="btn btn-danger" ng-click="$ctrl.revokeJoinLink()" ng-disabled="$ctrl.joinLinkBusy">Poista linkki käytöstä</button>
                        </div>
                        <div class="join-link-confirm" role="alertdialog" aria-label="Luo uusi linkki" ng-if="$ctrl.confirmingNewJoinLink">
                            <p>Vanha linkki lakkaa toimimasta. Jatketaanko?</p>
                            <div class="join-link-actions">
                                <button type="button" class="btn" ng-click="$ctrl.confirmingNewJoinLink = false">Peruuta</button>
                                <button type="button" class="btn btn-success" ng-click="$ctrl.createJoinLink()" ng-disabled="$ctrl.joinLinkBusy">Luo uusi</button>
                            </div>
                        </div>
                    </div>
                </section>

                <location-invite party-id="$ctrl.partyId"></location-invite>

                <h1>Lisää uusi bilettäjä</h1>

                <form class="form-horizontal" ng-submit="$ctrl.addUser()">
                    <div class="control-group">
                        <label class="control-label" for="type">Tyyppi</label>
                        <div class="controls">
                            <select id="type" ng-model="$ctrl.selectedUserTypeId" ng-options="userType.TypeId as userType.TypeName for userType in ::$ctrl.userTypes">
                            </select>
                        </div>
                    </div>

                    <div class="control-group" ng-if="$ctrl.selectedUserTypeId === 1">
                        <div ng-if="!$ctrl.inviting">
                            <div ng-repeat="invitation in $ctrl.store.invitations.data track by invitation.id" ng-click="$ctrl.inviteUser(invitation)" keyboard-click role="button" tabindex="0" class="party color{{ ($index % 12) + 1 }}" style="float: left; width: 200px; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; margin: 10px;">
                                <img ng-src="{{ invitation.gravatarUrl }}&size=30" />
                                {{ invitation.name }}
                            </div>
                        </div>
                        <div ng-if="$ctrl.inviting">
                            Kutsutaan ystävää {{ $ctrl.invitedFriend.name }}...
                        </div>
                    </div>

                    <div class="control-group" ng-if="$ctrl.selectedUserTypeId === 2">
                        <label class="control-label" for="email">Sähköposti</label>
                        <div class="controls">
                            <input id="email" type="email" ng-model="$ctrl.email" />
                        </div>
                    </div>

                    <div ng-if="$ctrl.selectedUserTypeId === 3">
                        <div class="control-group">
                            <label class="control-label" for="name">Nimi</label>
                            <div class="controls">
                                <input id="name" type="text" ng-model="$ctrl.name" />
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="sex">Sukupuoli</label>
                            <div class="controls">
                                <select id="sex" ng-model="$ctrl.sex">
                                    <option value="MALE">Mies</option>
                                    <option value="FEMALE">Nainen</option>
                                </select>
                            </div>
                        </div>

                        <div class="control-group">
                            <label class="control-label" for="weight">Paino</label>
                            <div class="controls">
                                <input id="weight" type="number" ng-model="$ctrl.weight" />
                            </div>
                        </div>
                    </div>

                    <div class="control-group" ng-if="$ctrl.selectedUserTypeId !== 1">
                        <div class="controls">
                            <button type="submit" class="btn btn-success">Lisää</button>
                        </div>
                    </div>
                </form>

                <h1>Poista bilettäjä</h1>

                <div class="row-fluid" ng-repeat="participant in $ctrl.store.participants.data track by participant.id" style="margin-bottom: 10px;">
                    <div class="span11">{{ participant.name }}</div>
                    <div class="span1"><button ng-click="$ctrl.removeUser(participant)" class="btn btn-danger">Poista</button></div>
                </div>
            </div>
        </div>
    </div>`
    });
})(angular);
