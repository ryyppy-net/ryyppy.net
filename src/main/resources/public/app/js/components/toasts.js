(function (angular) {
    'use strict';

    function ToastsCtrl(Notify) {
        this.toasts = Notify.toasts;
        this.pause = Notify.pause;
        this.resume = Notify.resume;
        this.close = Notify.dismiss;
    }

    ToastsCtrl.$inject = ['Notify'];

    angular.module('ryyppy').component('toasts', {
        controller: ToastsCtrl,
        bindings: {closeLabel: '@'},
        template:
            '<div class="toast-list" aria-live="polite">' +
            '<div class="toast-slot" ng-repeat="t in $ctrl.toasts track by t.id" ng-class="{\'toast-withdrawn\': t.withdrawn}">' +
            '<div class="toast-item" ng-mouseenter="$ctrl.pause(t)" ng-mouseleave="$ctrl.resume(t)">' +
            '<div class="toast-body">' +
            '<button type="button" class="toast-close" ng-click="$ctrl.close(t)" aria-label="{{ $ctrl.closeLabel }}">&times;</button>' +
            '<div class="toast-title">{{ t.title }}</div>' +
            '<div class="toast-text">{{ t.text }}</div>' +
            '<div class="toast-timer"></div>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '</div>'
    });
})(angular);
