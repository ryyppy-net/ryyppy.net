'use strict';

// Declare app level module which depends on filters, and services
angular.module('ryyppy', ['ngRoute', 'ngAnimate', 'ryyppy.filters', 'ryyppy.services', 'ryyppy.directives']).
  config(['$compileProvider', '$httpProvider', function($compileProvider, $httpProvider) {
    $compileProvider.debugInfoEnabled(false);
    $httpProvider.useApplyAsync(true);
  }]).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/', {template: '<user-page></user-page>'});
    $routeProvider.when('/profile-settings', {template: '<profile-settings-page></profile-settings-page>'});
    $routeProvider.when('/party/:partyId', {template: '<party-page></party-page>'});
    $routeProvider.when('/party-admin/', {template: '<general-party-admin-page></general-party-admin-page>'});
    $routeProvider.when('/party-admin/:partyId', {template: '<party-admin-page></party-admin-page>'});
    $routeProvider.otherwise({redirectTo: '/'});
  }]);
