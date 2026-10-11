function DrinkerCtrl($element, $timeout, RyyppyAPI, Sound, Notify) {
    "use strict";

    var self = this;


    this.addDefaultDrink = function (participant) {
        Sound.playSound();

        var defaultDrink = {volume: '0.33', alcohol: '0.047'};
        self.addDrink(participant, defaultDrink);
    };

    // The drink is saved on the tap; the overlay then offers a 5s window to
    // undo or edit it.
    this.addDrink = function (participant, drink) {
        self.showDrinkDialog = true;
        self.addingDrink = true;
        self.editingDrink = false;
        self.drink = drink;

        var i = 0;
        (function tick() {
            $element.find('.bar').css('width', i + '%');
            i++;

            if (i < 100) {
                setTimeout(tick, 40);
            }
        })();

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

    this.portionSizes = [
        { value: '0.04', text: '4 cl' },
        { value: '0.08', text: '8 cl' },
        { value: '0.12', text: '12 cl' },
        { value: '0.16', text: '16 cl' },
        { value: '0.2', text: '0.2 l' },
        { value: '0.33', text: '0.33 l' },
        { value: '0.4', text: '0.4 l' },
        { value: '0.5', text: '0.5 l' },
        { value: '0.6', text: '0.6 l' },
        { value: '1.0', text: '1.0 l' }
    ];
    this.selectedPortionSize = '0.33';

    this.portionAlcoholPercentages = [
        { value: '0.028', text: '2.8%' },
        { value: '0.047', text: '4.7%' },
        { value: '0.052', text: '5.2%' },
        { value: '0.055', text: '5.5%' },
        { value: '0.08', text: '8.0%' },
        { value: '0.12', text: '12.0%' },
        { value: '0.14', text: '14.0%' },
        { value: '0.22', text: '22.0%' },
        { value: '0.30', text: '30.0%' },
        { value: '0.32', text: '32.0%' },
        { value: '0.38', text: '38.0%' },
        { value: '0.40', text: '40.0%' },
        { value: '0.48', text: '48.0%' },
        { value: '0.80', text: '80.0%' }
    ];
    this.selectedAlcoholPercentage = '0.047';

    // Source: http://en.wikipedia.org/wiki/Toast_(honor)#Worldwide
    this.salutations = [
        'Gëzuar!',
        'Nazdravlje!',
        'Наздраве!',
        '干杯',
        'Na zdraví!',
        'Terviseks!',
        'Kippis!',
        'Hölökyn kölökyn!',
        'Cul sec!',
        'Saude!',
        'Prost!',
        '乾杯',
        'Sláinte!',
        'Skål!',
        'Slàinte mhath!',
        'Cheers!'
    ];

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

    function historyLoaded() {
        var histories = [];

        var rows = self.participant.history;
        for (var i = 0; i < rows.length; i++) {
            var row = rows[i];
            if (row.length === 0) {
                continue;
            }

            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();

            var history = [row.timestamp + timezoneoffset, row.promilles];
            histories.push(history);
        }

        var series = {data: histories, color: 'rgb(0, 0, 0)'};
        series = [series];

        var graphElement = $element.find('.drinker-graph');

        var graphOptions = {
            crosshair: {mode: null},
            yaxis: {min: 0},
            xaxis: {mode: "time", timeformat: "%H", show:true}
        };

        graphElement.show();
        $.plot(graphElement, series, graphOptions);
    }

    this.$onChanges = function (changes) {
        if (changes.participant && changes.participant.currentValue) {
            $timeout(historyLoaded, 0, false);
        }
    };

    this.$onDestroy = function () {
        $timeout.cancel(self.dismissTimeout);
    };
}

DrinkerCtrl.$inject = ['$element', '$timeout', 'RyyppyAPI', 'Sound', 'Notify'];

angular.module('ryyppy').component('drinker', {
    templateUrl: 'partials/user_button.html',
    controller: DrinkerCtrl,
    bindings: {
        participant: '<',
        onDrinksChanged: '&'
    }
});
