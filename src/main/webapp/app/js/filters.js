'use strict';

/* Filters */

angular.module('ryyppy.filters', []).
    filter('formatDateTime', [function() {
        return function(text) {
            return moment(text, 'MMM DD, YYYY h:mm:ss A').format("DD.MM.YY klo H");
        }
    }]).
    filter('formatISODateTime', [function() {
        return function(text) {
            return moment(text).format("DD.MM.YY klo H");
        }
    }]).
    filter('roundAmountOfShots', [function() {
        return function(amountOfShots) {
            return Math.round(amountOfShots);
        }
    }]);
