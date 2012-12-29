function DrinkerCtrl($scope, RyyppyAPI, Sound, Notify) {
    $scope.addDrink = function (participant) {
        console.log("Adding drink to " + participant.name);
        var drink = {"volume": 0.33, "alcohol": 0.5, "timestamp": null};

        if (participant.type == 'participant')
            RyyppyAPI.addDrink(participant.partyId, participant, drink, function (data) {
                Notify.success("New drink!", "Added a drink to " + participant.name + ".");
                Sound.playSound();
            });
        else
            RyyppyAPI.addDrinkToCurrentUser(drink, function (data) {
                Notify.success("New drink!", "Added a drink to you!");
                Sound.playSound();
            });
    };
}

DrinkerCtrl.$inject = ['$scope', 'RyyppyAPI', 'Sound', 'Notify'];