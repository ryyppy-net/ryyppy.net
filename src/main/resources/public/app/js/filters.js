(function (angular) {
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
        // "alle 100 m", "noin 300 m", "noin 1,5 km", "noin 2 km"
        filter('roughDistance', [function() {
            return function(distance) {
                if (distance.lessThan) {
                    return 'alle ' + distance.metres + ' m';
                }
                if (distance.metres < 1000) {
                    return 'noin ' + distance.metres + ' m';
                }
                return 'noin ' + String(distance.metres / 1000).replace('.', ',') + ' km';
            };
        }]).
        filter('roundAmountOfShots', [function() {
            return function(amountOfShots) {
                return Math.round(amountOfShots);
            }
        }]);
})(angular);
