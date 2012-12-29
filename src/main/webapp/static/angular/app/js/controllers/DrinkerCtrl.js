function DrinkerCtrl($scope, $rootScope, RyyppyAPI, Sound, Notify) {
    var self = this;


    this.drinkSuccessfullyAdded = function (participant, drink) {
        $rootScope.$broadcast('drinkAdded', participant, drink);
        Sound.playSound();
    };

    $scope.addDefaultDrink = function (participant) {
        var defaultDrink = {"volume": 0.33, "alcohol": 0.05, "timestamp": null};
        $scope.participant = participant;
        this.addDrink(participant, defaultDrink);
    };

    $scope.addEditedDrink = function () {
        var editedDrink = {"volume": $scope.selectedPortionSize, "alcohol": $scope.selectedAlcoholPercentage, "timestamp": null};
        this.addDrink($scope.participant, editedDrink);
    };

    this.addDrink = function (participant, drink) {
        $scope.showDrinkDialog = true;
        $scope.addingDrink = true;
        $scope.editingDrink = false;
        $scope.drink = drink;

        this.timeoutId = setTimeout($.proxy(function() {
            if (participant.type == 'participant')
                RyyppyAPI.addDrink(participant.partyId, participant, drink, function (data) {
                    Notify.success("New drink!", "Added a drink to " + participant.name + ".");
                    self.drinkSuccessfullyAdded(participant, drink);
                });
            else
                RyyppyAPI.addDrinkToCurrentUser(drink, function (data) {
                    Notify.success("New drink!", "Added a drink to you!");
                    self.drinkSuccessfullyAdded(participant, drink);
                });

            $scope.hideDialog();
        }, this), 5000);
    };

    $scope.editDrink = function() {
        clearTimeout(this.timeoutId);
        $scope.editingDrink = true;
        $scope.addingDrink = false;
    };

    $scope.cancelDrink = function () {
        clearTimeout(this.timeoutId);
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
    $scope.selectedPortionSize = 0.04;

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
    $scope.selectedAlcoholPercentage = 0.047;
}

DrinkerCtrl.$inject = ['$scope', '$rootScope', 'RyyppyAPI', 'Sound', 'Notify'];