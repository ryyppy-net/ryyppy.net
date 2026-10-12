'use strict';

function ProfileSettingsPageCtrl($location, RyyppyAPI, Notify) {
    var self = this;

    this.updateProfile = function () {
        var profile = {
            name: self.name,
            email: self.email,
            sex: self.sex,
            weight: angular.isNumber(self.weight) ? self.weight : self.profile.weight
        };

        RyyppyAPI.updateProfile(profile, function () {
            Notify.success("Tiedot päivitetty!", "Uudet asetukset tallennettu.");
            $location.path("/");
        });
    };

    this.$onInit = function () {
        RyyppyAPI.getProfile(function (data) {
            self.profile = data;

            self.name = data.name;
            self.email = data.email;
            self.sex = data.sex;
            self.weight = null;
        });
    };
}

ProfileSettingsPageCtrl.$inject = ['$location', 'RyyppyAPI', 'Notify'];

angular.module('ryyppy').component('profileSettingsPage', {
    controller: ProfileSettingsPageCtrl,
    template: `
<user-menu active="profile-settings"></user-menu>

<div class="container-fluid">
    <div class="row-fluid">
        <div class="span10 offset1">
            <h1>Muokkaa tietojasi</h1>

            <p>
                Sähköpostiosoitteellasi kaverit voivat kutsua sinut bileisiin.
                Jos käytät <a href="https://www.gravatar.com">Gravatar</a>-palvelua, Gravatar-profiilikuvasi näytetään kavereillesi ryyppy.netissä.
                Me emme lähetä sinulle sähköpostia tai anna sähköpostiosoitettasi eteenpäin.
            </p>

            <form class="form-horizontal" ng-submit="$ctrl.updateProfile()">
                <div class="control-group">
                    <label class="control-label" for="name">Nimi</label>
                    <div class="controls">
                        <input id="name" type="text" ng-model="$ctrl.name" />
                    </div>
                </div>

                <div class="control-group">
                    <label class="control-label" for="email">Sähköposti</label>
                    <div class="controls">
                        <input id="email" type="email" ng-model="$ctrl.email" />
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

                <div class="control-group">
                    <div class="controls">
                        <button type="submit" class="btn btn-success">Tallenna</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
</div>`
});
