(function (angular) {
    'use strict';

    function DrinkerCtrl($element, DrinkerChart, DrinkOptions, Drinks, Sound, Notify) {
        var self = this;


        this.addDefaultDrink = function (participant) {
            Sound.playSound();

            var defaultDrink = {volume: DrinkOptions.defaultPortionSize, alcohol: DrinkOptions.defaultAlcoholPercentage};
            self.addDrink(participant, defaultDrink);
        };

        // The drink is saved on the tap; the dialog then offers a window to undo
        // or edit it.
        this.addDrink = function (participant, drink) {
            var added = {drink: drink};
            added.saved = Drinks.add(participant, drink).then(function (saved) {
                var message = participant.type === 'participant'
                    ? "Käyttäjälle " + participant.name + " lisättiin juoma."
                    : "Sinulle lisättiin juoma.";
                added.notice = Notify.success(self.getRandomSalutation(), message);
                return saved;
            });
            self.addedDrink = added;
        };

        this.salutations = DrinkOptions.salutations;

        this.getRandomSalutation = function () {
            var salutation = self.salutations[Math.floor(Math.random() * self.salutations.length)];
            return salutation;
        };

        this.$postLink = function () {
            var canvas = $element[0].querySelector('.drinker-graph canvas');
            self.chart = new DrinkerChart(canvas, self.participant);
        };

        this.$onChanges = function (changes) {
            if (changes.participant && changes.participant.currentValue && self.chart) {
                self.chart.update(self.participant);
            }
        };

        this.$onDestroy = function () {
            if (self.chart) {
                self.chart.destroy();
            }
        };
    }

    DrinkerCtrl.$inject = ['$element', 'DrinkerChart', 'DrinkOptions', 'Drinks', 'Sound', 'Notify'];

    angular.module('ryyppy').component('drinker', {
        controller: DrinkerCtrl,
        bindings: {
            participant: '<'
        },
        template: `
    <div class="drinker color{{ $ctrl.participant.color }}" style="position: relative;">
        <div ng-click="$ctrl.addDefaultDrink($ctrl.participant)" keyboard-click role="button" tabindex="0" class="container-fluid" style="margin-top: 10px;">
            <div class="row-fluid">
                <div class="span4" style="text-align: center;">
                    <img ng-src="{{ $ctrl.participant.profilePictureUrl }}" />
                    <p>{{ $ctrl.participant.name }}</p>
                </div>

                <div class="span8">
                    <div class="row-fluid">
                        <div class="span12">
                            <div class="drinker-graph" style="height: 100px;"><canvas></canvas></div>
                        </div>
                    </div>

                    <div class="row-fluid">
                        <div class="span12" style="text-align: center;">
                            <p>Juomia: {{ $ctrl.participant.totalDrinks }} kpl</p>
                            <p>Promilleja:  {{ $ctrl.participant.promilles | number: 2 }}&permil;</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <drink-dialog participant="$ctrl.participant" added-drink="$ctrl.addedDrink"></drink-dialog>
    </div>`
    });
})(angular);
