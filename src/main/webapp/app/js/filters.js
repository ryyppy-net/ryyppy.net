'use strict';

/* Filters */

angular.module('ryyppy.filters', []).
    filter('formatDateTime', [function() {
        return function(text) {
            return moment(text, 'MMM DD, YYYY h:mm:ss A').format("DD.MM.YY klo H");
        }
    }]);
