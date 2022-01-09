'use strict';

/* Directives */


angular.module('ryyppy.directives', []).
    directive('participantPreview', [function() {
        return function(scope, elm, attrs) {
            if (typeof scope.participantPreview.profilePictureUrl !== undefined)
            {
                elm.html('<img src="' + scope.participantPreview.profilePictureUrl + '&size=30" title="' + scope.participantPreview.name + '" />');
            }
        };
    }]);
