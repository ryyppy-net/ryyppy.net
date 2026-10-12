(function (angular) {
    'use strict';

    function DrinkDialogCtrl($timeout, DrinkOptions, Drinks, Notify) {
        var self = this;

        this.portionSizes = DrinkOptions.portionSizes;
        this.selectedPortionSize = DrinkOptions.defaultPortionSize;
        this.portionAlcoholPercentages = DrinkOptions.portionAlcoholPercentages;
        this.selectedAlcoholPercentage = DrinkOptions.defaultAlcoholPercentage;

        this.showDrinkDialog = false;
        this.addingDrink = false;
        this.editingDrink = false;

        // The drink is saved on the tap; the overlay then offers a 5s window to
        // undo or edit it.
        this.$onChanges = function (changes) {
            if (!changes.addedDrink || !changes.addedDrink.currentValue) {
                return;
            }
            self.showDrinkDialog = true;
            self.addingDrink = true;
            self.editingDrink = false;
            self.drink = self.addedDrink.drink;

            self.dismissTimeout = $timeout(self.hideDialog, 5000);
        };

        this.editDrink = function () {
            $timeout.cancel(self.dismissTimeout);
            self.editingDrink = true;
            self.addingDrink = false;
        };

        this.saveEditedDrink = function () {
            var participant = self.participant;
            var added = self.addedDrink;
            var editedDrink = {volume: self.selectedPortionSize, alcohol: self.selectedAlcoholPercentage};
            added.saved.then(function (saved) {
                return Drinks.change(participant, saved.id, editedDrink);
            });
            self.hideDialog();
        };

        this.cancelDrink = function () {
            $timeout.cancel(self.dismissTimeout);
            var participant = self.participant;
            var added = self.addedDrink;
            added.saved.then(function (saved) {
                Notify.remove(added.notice);
                return Drinks.remove(participant, saved.id);
            });
            self.hideDialog();
        };

        this.hideDialog = function () {
            self.showDrinkDialog = false;
            self.editingDrink = false;
            self.addingDrink = false;
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

        this.$onDestroy = function () {
            $timeout.cancel(self.dismissTimeout);
        };
    }

    DrinkDialogCtrl.$inject = ['$timeout', 'DrinkOptions', 'Drinks', 'Notify'];

    angular.module('ryyppy').component('drinkDialog', {
        controller: DrinkDialogCtrl,
        bindings: {
            participant: '<',
            addedDrink: '<'
        },
        template: `
        <div ng-if="$ctrl.showDrinkDialog && $ctrl.addingDrink" class="drinker-overlay">
            <p>Lisättiin juoma käyttäjälle {{ $ctrl.participant.name }}.</p>

            <p>Juoman koko on {{ $ctrl.formattedAlcoholSize() }} ja vahvuus {{ $ctrl.formattedAlcoholPercentage() }}.</p>
            <div class="progress" style="margin-left: 10px; margin-right: 10px;">
                <div class="bar" ng-class="{'undo-countdown': $ctrl.addingDrink}"></div>
            </div>

            <button type="button" class="btn" ng-click="$ctrl.editDrink()">Muokkaa</button>
            <button type="button" class="btn btn-danger" ng-click="$ctrl.cancelDrink()">Peruuta</button>
        </div>

        <div ng-if="$ctrl.showDrinkDialog && $ctrl.editingDrink" class="drinker-overlay">
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
                    <button type="button" class="btn btn-danger" ng-click="$ctrl.cancelDrink()">Peruuta</button>
                </div>
            </form>
        </div>`
    });
})(angular);
