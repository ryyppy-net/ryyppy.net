(function (angular) {
    'use strict';

    function DrinkerCtrl($element, $timeout, DrinkerChart, DrinkOptions, RyyppyAPI, Sound, Notify) {
        var self = this;


        this.addDefaultDrink = function (participant) {
            Sound.playSound();

            var defaultDrink = {volume: DrinkOptions.defaultPortionSize, alcohol: DrinkOptions.defaultAlcoholPercentage};
            self.addDrink(participant, defaultDrink);
        };

        // The drink is saved on the tap; the overlay then offers a 5s window to
        // undo or edit it.
        this.addDrink = function (participant, drink) {
            self.showDrinkDialog = true;
            self.addingDrink = true;
            self.editingDrink = false;
            self.drink = drink;

            self.savedDrink = RyyppyAPI.addDrink(participant, drink).then(function (saved) {
                var message = participant.type === 'participant'
                    ? "Käyttäjälle " + participant.name + " lisättiin juoma."
                    : "Sinulle lisättiin juoma.";
                self.addedNotice = Notify.success(self.getRandomSalutation(), message);
                self.onDrinksChanged({participant: participant});
                return saved;
            });

            self.dismissTimeout = $timeout(self.hideDialog, 5000);
        };

        this.editDrink = function () {
            $timeout.cancel(self.dismissTimeout);
            self.editingDrink = true;
            self.addingDrink = false;
        };

        this.saveEditedDrink = function () {
            var participant = self.participant;
            var editedDrink = {volume: self.selectedPortionSize, alcohol: self.selectedAlcoholPercentage};
            self.savedDrink.then(function (saved) {
                return RyyppyAPI.changeDrink(participant, saved.id, editedDrink);
            }).then(function () {
                self.onDrinksChanged({participant: participant});
            });
            self.hideDialog();
        };

        this.cancelDrink = function () {
            $timeout.cancel(self.dismissTimeout);
            var participant = self.participant;
            self.savedDrink.then(function (saved) {
                Notify.remove(self.addedNotice);
                return RyyppyAPI.deleteDrink(participant, saved.id);
            }).then(function () {
                self.onDrinksChanged({participant: participant});
            });
            self.hideDialog();
        };

        this.hideDialog = function () {
            self.showDrinkDialog = false;
            self.editingDrink = false;
            self.addingDrink = false;
        };


        // Initialization:
        this.addingDrink = false;
        this.editingDrink = false;

        this.portionSizes = DrinkOptions.portionSizes;
        this.selectedPortionSize = DrinkOptions.defaultPortionSize;

        this.portionAlcoholPercentages = DrinkOptions.portionAlcoholPercentages;
        this.selectedAlcoholPercentage = DrinkOptions.defaultAlcoholPercentage;

        this.salutations = DrinkOptions.salutations;

        this.getRandomSalutation = function () {
            var salutation = self.salutations[Math.floor(Math.random() * self.salutations.length)];
            return salutation;
        };

        this.formattedAlcoholSize = function () {
            if (typeof self.drink === 'undefined') {
                return '';
            }

            for (var i = 0; i < self.portionSizes.length; i++) {
                if (self.portionSizes[i].value === self.drink.volume) {
                    return self.portionSizes[i].text;
                }
            }
            return '???';
        };

        this.formattedAlcoholPercentage = function () {
            if (typeof self.drink === 'undefined') {
                return '';
            }

            for (var i = 0; i < self.portionAlcoholPercentages.length; i++) {
                if (self.portionAlcoholPercentages[i].value === self.selectedAlcoholPercentage) {
                    return self.portionAlcoholPercentages[i].text;
                }
            }
            return '???';
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
            $timeout.cancel(self.dismissTimeout);
            if (self.chart) {
                self.chart.destroy();
            }
        };
    }

    DrinkerCtrl.$inject = ['$element', '$timeout', 'DrinkerChart', 'DrinkOptions', 'RyyppyAPI', 'Sound', 'Notify'];

    angular.module('ryyppy').component('drinker', {
        controller: DrinkerCtrl,
        bindings: {
            participant: '<',
            onDrinksChanged: '&'
        },
        template: `
    <div class="drinker color{{ $ctrl.participant.color }}" style="position: relative;">
        <div ng-click="$ctrl.addDefaultDrink($ctrl.participant)" class="container-fluid" style="margin-top: 10px;">
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

        <div ng-show="$ctrl.showDrinkDialog && $ctrl.addingDrink" class="drinker-overlay">
            <p>Lisättiin juoma käyttäjälle {{ $ctrl.participant.name }}.</p>

            <p>Juoman koko on {{ $ctrl.formattedAlcoholSize() }} ja vahvuus {{ $ctrl.formattedAlcoholPercentage() }}.</p>
            <div class="progress" style="margin-left: 10px; margin-right: 10px;">
                <div class="bar" ng-class="{'undo-countdown': $ctrl.addingDrink}"></div>
            </div>

            <div class="btn" ng-click="$ctrl.editDrink()">Muokkaa</div>
            <div class="btn btn-danger" ng-click="$ctrl.cancelDrink()">Peruuta</div>
        </div>

        <div ng-show="$ctrl.showDrinkDialog && $ctrl.editingDrink" class="drinker-overlay">
            <p>Muokkaa käyttäjän {{ $ctrl.participant.name }} juomaa</p>

            <form ng-submit="$ctrl.saveEditedDrink()" class="edit-drink">
                <div style="clear: both;">
                    <label for="portionSize" class="edit-drink-label">Annoskoko</label>
                    <select id="portionSize"
                            ng-model="$ctrl.selectedPortionSize"
                            ng-options="portionSize.value as portionSize.text for portionSize in ::$ctrl.portionSizes"
                            class="edit-drink-input">
                    </select>
                </div>

                <div style="clear: both;">
                    <label for="portionAlcoholPercentage" class="edit-drink-label">Alkoholi-%</label>
                    <select id="portionAlcoholPercentage"
                            ng-model="$ctrl.selectedAlcoholPercentage"
                            ng-options="portionAlcoholPercentage.value as portionAlcoholPercentage.text for portionAlcoholPercentage in ::$ctrl.portionAlcoholPercentages"
                            class="edit-drink-input">
                    </select>
                </div>

                <div style="clear: both">
                    <button type="submit" class="btn btn-success">Tallenna</button>
                    <div class="btn btn-danger" ng-click="$ctrl.cancelDrink()">Peruuta</div>
                </div>
            </form>
        </div>
    </div>`
    });
})(angular);
