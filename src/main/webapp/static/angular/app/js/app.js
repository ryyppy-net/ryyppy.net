'use strict';


// Declare app level module which depends on filters, and services
angular.module('ryyppy', ['ryyppy.filters', 'ryyppy.services', 'ryyppy.directives']).
  config(['$routeProvider', function($routeProvider) {
    $routeProvider.when('/view1', {templateUrl: 'partials/user.html', controller: UserCtrl});
    $routeProvider.when('/party/:partyId', {templateUrl: 'partials/party.html', controller: MyCtrl2});
    $routeProvider.otherwise({redirectTo: '/view1'});
  }]);
