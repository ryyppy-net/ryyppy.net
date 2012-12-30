function DrinkerCtrl($scope, $rootScope, RyyppyAPI, Sound, Notify) {
    "use strict";

    var self = this;


    this.drinkSuccessfullyAdded = function (participant, drink) {
        $rootScope.$broadcast('drinkAdded', participant, drink);
        Sound.playSound();
    };

    $scope.addDefaultDrink = function (participant) {
        var defaultDrink = {volume: '0.33', alcohol: '0.047', timestamp: null};
        $scope.participant = participant;
        self.addDrink(participant, defaultDrink);
    };

    $scope.addEditedDrink = function () {
        var editedDrink = {volume: $scope.selectedPortionSize, alcohol: $scope.selectedAlcoholPercentage, timestamp: null};
        self.addDrink($scope.participant, editedDrink);
    };

    this.addDrink = function (participant, drink) {
        $scope.showDrinkDialog = true;
        $scope.addingDrink = true;
        $scope.editingDrink = false;
        $scope.drink = drink;

        var i = 0;
        (function tick() {
            $('.bar').css('width', i + '%');
            i++;

            if (i < 100) {
                setTimeout(tick, 40);
            }
        })();

        self.timeoutId = setTimeout(function () {
            if (participant.type === 'participant') {
                RyyppyAPI.addDrink(participant.partyId, participant, drink, function () {
                    Notify.success(self.getRandomSalutation(), "Käyttäjälle " + participant.name + " lisättiin juoma.");
                    self.drinkSuccessfullyAdded(participant, drink);
                });
            }
            else {
                RyyppyAPI.addDrinkToCurrentUser(drink, function () {
                    Notify.success(self.getRandomSalutation(), "Sinulle lisättiin juoma.");
                    self.drinkSuccessfullyAdded(participant, drink);
                });
            }
            $scope.hideDialog();
        }, 5000);
    };

    $scope.editDrink = function() {
        clearTimeout(self.timeoutId);
        $scope.editingDrink = true;
        $scope.addingDrink = false;
    };

    $scope.cancelDrink = function () {
        clearTimeout(self.timeoutId);
        $scope.hideDialog();
    };

    $scope.hideDialog = function () {
        $scope.showDrinkDialog = false;
        $scope.editingDrink = false;
        $scope.addingDrink = false;
    };


    // Initialization:
    $scope.addDrink = this.addDrink;
    $scope.addingDrink = false;
    $scope.editingDrink = false;

    $scope.portionSizes = [
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
    $scope.selectedPortionSize = '0.33';

    $scope.portionAlcoholPercentages = [
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
    $scope.selectedAlcoholPercentage = '0.047';

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

    $scope.formattedAlcoholSize = function () {
        if (typeof $scope.drink === 'undefined') {
            return '';
        }

        for (var i = 0; i < $scope.portionSizes.length; i++) {
            if ($scope.portionSizes[i].value === $scope.drink.volume) {
                return $scope.portionSizes[i].text;
            }
        }
        return '???';
    };

    $scope.formattedAlcoholPercentage = function () {
        if (typeof $scope.drink === 'undefined') {
            return '';
        }

        for (var i = 0; i < $scope.portionAlcoholPercentages.length; i++) {
            if ($scope.portionAlcoholPercentages[i].value === $scope.selectedAlcoholPercentage) {
                return $scope.portionAlcoholPercentages[i].text;
            }
        }
        return '???';
    };

    function historyLoaded() {
        var histories = [];

        var rows = $scope.participant.history;
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

        var graphElement = $('#graph' + $scope.participant.id);
        if (typeof graphElement === 'undefined') {
            return;
        }

        if (series === null) {
            return;
        }

        var graphOptions = {
            crosshair: {mode: null},
            yaxis: {min: 0},
            xaxis: {mode: "time", timeformat: "%H", show:true}
        };

        graphElement.show();
        $.plot(graphElement, series, graphOptions);
    }

    setTimeout(function () {
        historyLoaded();
    }, 0);
}

DrinkerCtrl.$inject = ['$scope', '$rootScope', 'RyyppyAPI', 'Sound', 'Notify'];