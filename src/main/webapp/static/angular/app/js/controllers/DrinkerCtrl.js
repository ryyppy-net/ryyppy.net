function DrinkerCtrl($scope, $rootScope, RyyppyAPI, Sound, Notify) {
    var self = this;


    this.drinkSuccessfullyAdded = function (participant, drink) {
        $rootScope.$broadcast('drinkAdded', participant, drink);
        Sound.playSound();
    };

    this.addDrink = function (participant) {
        $scope.showFreeSpirit = true;

        this.timeoutId = setTimeout($.proxy(function() {
            var drink = {"volume": 0.33, "alcohol": 0.5, "timestamp": null};

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

    $scope.cancelDrink = function () {
        clearTimeout(this.timeoutId);
        $scope.hideDialog();
    };

    $scope.hideDialog = function () {
        $scope.showFreeSpirit = false;
    };


    // Initialization:
    $scope.addDrink = this.addDrink;
}

DrinkerCtrl.$inject = ['$scope', '$rootScope', 'RyyppyAPI', 'Sound', 'Notify'];