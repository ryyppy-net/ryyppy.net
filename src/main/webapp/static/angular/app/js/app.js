'use strict';

// Declare app level module which depends on filters, and services
angular.module('ryyppy', ['ryyppy.filters', 'ryyppy.services', 'ryyppy.directives']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/', {templateUrl: 'partials/user.html', controller: UserCtrl});
    $routeProvider.when('/profile-settings', {templateUrl: 'partials/profile_settings.html', controller: ProfileSettingsCtrl});
    $routeProvider.when('/party/:partyId', {templateUrl: 'partials/party.html', controller: PartyCtrl});
    $routeProvider.when('/party-admin/:partyId', {templateUrl: 'partials/party_admin.html', controller: PartyAdminCtrl});
    $routeProvider.otherwise({redirectTo: '/'});
  }]);
