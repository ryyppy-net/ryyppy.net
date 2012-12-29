function DrinkerCtrl($scope, $rootScope, RyyppyAPI, Sound, Notify) {
    $scope.addDrink = function (participant) {
        var drink = {"volume": 0.33, "alcohol": 0.5, "timestamp": null};

        if (participant.type == 'participant')
            RyyppyAPI.addDrink(participant.partyId, participant, drink, function (data) {
                Notify.success("New drink!", "Added a drink to " + participant.name + ".");
                $rootScope.$broadcast('drinkAdded', participant, drink);
                Sound.playSound();
            });
        else
            RyyppyAPI.addDrinkToCurrentUser(drink, function (data) {
                Notify.success("New drink!", "Added a drink to you!");
                $rootScope.$broadcast('drinkAdded', participant, drink);
                Sound.playSound();
            });
    };
}

DrinkerCtrl.$inject = ['$scope', '$rootScope', 'RyyppyAPI', 'Sound', 'Notify'];