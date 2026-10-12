'use strict';

/* Directives */

angular.module('ryyppy.directives', []).
    // Makes Enter and Space on a focused element trigger its ng-click, as they
    // do on a <button>. The element still needs role="button" and tabindex="0".
    directive('keyboardClick', [function() {
        return {
            restrict: 'A',
            link: function(scope, element) {
                element.on('keydown', function(event) {
                    if (event.target !== element[0]) {
                        return;
                    }
                    if (event.key === 'Enter' || event.key === ' ') {
                        event.preventDefault();
                        element.triggerHandler('click');
                    }
                });
            }
        };
    }]);
