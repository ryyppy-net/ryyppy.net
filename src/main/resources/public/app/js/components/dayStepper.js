(function (angular) {
    'use strict';

    // A whole number of days, at least 1, changed one day at a time.
    angular.module('ryyppy').component('dayStepper', {
        bindings: {days: '<', onChange: '&'},
        controller: function () {
            var self = this;

            this.step = function (delta) {
                self.onChange({days: Math.max(1, self.days + delta)});
            };
        },
        template: `
    <div class="day-stepper input-prepend input-append">
        <button type="button" class="btn" aria-label="Lyhennä vuorokaudella" ng-disabled="$ctrl.days <= 1" ng-click="$ctrl.step(-1)">&minus;</button>
        <output class="add-on day-stepper-value">{{ $ctrl.days }} vrk</output>
        <button type="button" class="btn" aria-label="Pidennä vuorokaudella" ng-click="$ctrl.step(1)">+</button>
    </div>`
    });
})(angular);
