(function (angular) {
    'use strict';

    angular.module('ryyppy').component('userMenu', {
        bindings: {active: '@'},
        controller: ['$http', '$window', function ($http, $window) {
            this.logout = function ($event) {
                $event.preventDefault();
                $http.post('/logout').finally(function () {
                    $window.location.href = '/ui/login?logout';
                });
            };
        }],
        template: `
    <div class="navbar hidden-phone">
        <div class="navbar-inner">
            <span class="brand">Ryyppy.net</span>
            <ul class="nav">
                <li ng-class="$ctrl.active == 'user' && 'active'"><a href="/app/">Sinä</a></li>
                <li ng-class="$ctrl.active == 'general-party-admin' && 'active'"><a href="/app/party-admin">Bileiden hallinta</a></li>
                <li ng-class="$ctrl.active == 'profile-settings' && 'active'"><a href="/app/profile-settings">Asetukset</a></li>
            </ul>
            <ul class="nav pull-right">
                <li><a href="/ui/user" title="Vaihda vanhaan käyttöliittymään">Vanha käyttöliittymä</a></li>
                <li><a href="#" class="g_id_signout" ng-click="$ctrl.logout($event)">Kirjaudu ulos</a></li>
            </ul>
        </div>
    </div>

    <div class="mobile-navbar navbar visible-phone">
        <div class="navbar-inner">
            <ul class="nav">
                <li ng-class="$ctrl.active == 'user' && 'active'"><a href="/app/"><i class="icon-home"></i></a></li>
                <li ng-class="$ctrl.active == 'general-party-admin' && 'active'"><a href="/app/party-admin"><i class="icon-group"></i></a></li>
                <li ng-class="$ctrl.active == 'profile-settings' && 'active'"><a href="/app/profile-settings"><i class="icon-cog"></i></a></li>
            </ul>
            <ul class="nav mobile-nav pull-right">
                <li><a href="/ui/user" title="Vaihda vanhaan käyttöliittymään"><i class="icon-share"></i></a></li>
                <li><a href="#" class="g_id_signout" ng-click="$ctrl.logout($event)"><i class="icon-signout"></i></a></li>
            </ul>
        </div>
    </div>`
    });
})(angular);
