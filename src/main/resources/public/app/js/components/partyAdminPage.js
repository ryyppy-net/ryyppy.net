'use strict';

function PartyAdminPageCtrl($routeParams, $location, RyyppyAPI, Notify) {
    var self = this;

    this._updatePartyInformation = function () {
        RyyppyAPI.getParty($routeParams.partyId, function (data) {
            self.party = data;
        });
    };

    this._updatePartyParticipants = function () {
        RyyppyAPI.getPartyParticipants($routeParams.partyId, function (data) {
            self.participants = data;
        });
    };

    this._updatePartyInvitations = function () {
        RyyppyAPI.getPartyInvitations($routeParams.partyId, function (data) {
            self.invitations = data;
        });
    };

    this.addUser = function () {
        if (self.selectedUserTypeId == 2)
            self._addRegisteredUser($routeParams.partyId, self.email);
        if (self.selectedUserTypeId == 3)
            self._addGuestUser(self.name, self.sex, self.weight);
    };

    this._addRegisteredUser = function (partyId, email) {
        RyyppyAPI.addRegisteredUserToParty(partyId, email, function (data) {
            Notify.success("Tervetuloa takaisin!", "Lisätty käyttäjä sähköpostiosoitteella " + email + ".");
            self._updatePartyParticipants();
            self._updatePartyInvitations();
        });
    };

    this._addGuestUser = function (name, sex, weight) {
        var guest = { name: name, sex: sex, weight: weight };
        RyyppyAPI.addGuestToParty($routeParams.partyId, guest, function (data) {
            Notify.success("Tervetuloa vieraalle!", "Lisätty vieras " + self.name + ".");
            self._updatePartyParticipants();
        });
    }

    this.inviteUser = function (invitation) {
        self.inviting = true;
        self.invitedFriend = invitation;
        RyyppyAPI.inviteUser(invitation.id, $routeParams.partyId, function () {
            self.invitedFriend = undefined;
            Notify.success("Pitkästä aikaa!", "Lisätty käyttäjä " + invitation.name + ".");
            self._updatePartyParticipants();
            self._updatePartyInvitations();
            self.inviting = false;
        });
    }

    this.removeUser = function (participant) {
        RyyppyAPI.removeUser($routeParams.partyId, participant, function (data) {
            Notify.success("ULOS!", "Heitettiin " + participant.name + " pihalle.");
            // Only party members may read the party, so after leaving it there is nothing to show here.
            if (self.profile && participant.id === self.profile.id) {
                $location.path('/party-admin/');
                return;
            }
            self._updatePartyParticipants();
            self._updatePartyInvitations();
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

        RyyppyAPI.getProfile(function (data) {
            self.profile = data;
        });
        self._updatePartyInformation();
        self._updatePartyParticipants();
        self._updatePartyInvitations();
    };
}

PartyAdminPageCtrl.$inject = ['$routeParams', '$location', 'RyyppyAPI', 'Notify'];

angular.module('ryyppy').component('partyAdminPage', {
    controller: PartyAdminPageCtrl,
    template: `
<party-menu active="admin" party="$ctrl.party"></party-menu>

<div class="container-fluid">
    <div class="row-fluid">
        <div class="span10 offset1">
            <h1>Lisää uusi bilettäjä</h1>

            <form class="form-horizontal" ng-submit="$ctrl.addUser()">
                <div class="control-group">
                    <label class="control-label" for="type">Tyyppi</label>
                    <div class="controls">
                        <select id="type" ng-model="$ctrl.selectedUserTypeId" ng-options="userType.TypeId as userType.TypeName for userType in ::$ctrl.userTypes">
                        </select>
                    </div>
                </div>

                <div class="control-group" ng-if="$ctrl.selectedUserTypeId == 1">
                    <div ng-if="$ctrl.inviting == false">
                        <div ng-repeat="invitation in $ctrl.invitations" ng-click="$ctrl.inviteUser(invitation)" class="party color{{ ($index % 12) + 1 }}" style="float: left; width: 200px; text-overflow: ellipsis; white-space: nowrap; overflow: hidden; margin: 10px;">
                            <img ng-src="{{ invitation.gravatarUrl }}&size=30" />
                            {{ invitation.name }}
                        </div>
                    </div>
                    <div ng-if="$ctrl.inviting">
                        Kutsutaan ystävää {{ $ctrl.invitedFriend.name }}...
                    </div>
                </div>

                <div class="control-group" ng-if="$ctrl.selectedUserTypeId == 2">
                    <label class="control-label" for="email">Sähköposti</label>
                    <div class="controls">
                        <input id="email" type="email" ng-model="$ctrl.email" />
                    </div>
                </div>

                <div ng-if="$ctrl.selectedUserTypeId == 3">
                    <div class="control-group">
                        <label class="control-label" for="name">Nimi</label>
                        <div class="controls">
                            <input id="name" type="text" ng-model="$ctrl.name" />
                        </div>
                    </div>

                    <div class="control-group">
                        <label class="control-label" for="weight">Sukupuoli</label>
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
                            <input id="weight" type="password" ng-model="$ctrl.weight" />
                        </div>
                    </div>
                </div>

                <div class="control-group" ng-if="$ctrl.selectedUserTypeId != 1">
                    <div class="controls">
                        <button type="submit" class="btn btn-success">Lisää</button>
                    </div>
                </div>
            </form>

            <h1>Poista bilettäjä</h1>

            <div class="row-fluid" ng-repeat="participant in $ctrl.participants" style="margin-bottom: 10px;">
                <div class="span11">{{ participant.name }}</div>
                <div class="span1"><button ng-click="$ctrl.removeUser(participant)" class="btn btn-danger">Poista</button></div>
            </div>
        </div>
    </div>
</div>`
});
