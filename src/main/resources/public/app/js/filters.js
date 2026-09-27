'use strict';

/* Filters */

function formatFinnishDateTime(text) {
    var date = new Date(text);
    var pad = function (n) { return (n < 10 ? '0' : '') + n; };
    return pad(date.getDate()) + '.' + pad(date.getMonth() + 1) + '.' + pad(date.getFullYear() % 100)
        + ' klo ' + date.getHours();
}

angular.module('ryyppy.filters', []).
    filter('formatDateTime', [function() {
        return formatFinnishDateTime;
    }]).
    filter('formatISODateTime', [function() {
        return formatFinnishDateTime;
    }]).
    filter('roundAmountOfShots', [function() {
        return function(amountOfShots) {
            return Math.round(amountOfShots);
        }
    }]);
