'use strict';

/* Controllers */


function UserCtrl($scope, RyyppyAPI) {
    RyyppyAPI.getProfile(function (data) {
        $scope.profile = data;
    });

    RyyppyAPI.getParties(function (data) {
        $scope.parties = data;
    });
}
UserCtrl.$inject = ['$scope', 'RyyppyAPI'];


function PartyCtrl($scope, $routeParams, $timeout, RyyppyAPI) {
    var self = this;
    $scope.active = "party";

    RyyppyAPI.getParty($routeParams.partyId, function (data) {
        $scope.party = data;
    });

    (function tick() {
        RyyppyAPI.getPartyParticipants($routeParams.partyId, function (data) {
            $scope.participants = data;

            var rowsAmount = Math.ceil(data.length / 3);
            var rows = new Array(rowsAmount);
            for (var i = 0; i < rowsAmount; i++) {
                var colsAmount = Math.min(3, data.length - i * 3);
                var cols = new Array(colsAmount);
                rows[i] = cols;
                for (var j = 0; j < cols.length; j++) {
                    cols[j] = data[i * 3 + j];
                }
            }
            $scope.rows = rows;
        });
        console.log("Polling...");
        self.timeoutPromise = $timeout(tick, 60000);
    })();

    $scope.$on('$destroy', function cleanup() {
        $timeout.cancel(self.timeoutPromise);
    });

    $scope.addDrink = function (participant) {
        console.log("Adding drink to " + participant.name);
        var drink = {"volume": 0.33, "alcohol": 0.5, "timestamp": null};
        RyyppyAPI.addDrink($routeParams.partyId, participant, drink, function (data) {
            console.log("Added drink.");
            $scope.successMessage = "Added a drink to " + participant.name;
            self.playSound();
            $timeout(function () {
                $scope.successMessage = null;
            }, 5000);
        });
    };

    this.playSound = function () {
        var audio = document.createElement("audio");
        var source = document.createElement('source');
        var filename = "/static/sounds/" + Math.floor(Math.random() * 7 + 1);

        if (!audio.canPlayType) return; // no html5 audio support

        if (navigator.userAgent.indexOf("Opera M") !== -1) { // stupid buggy opera mobile
            source.type= 'audio/wav';
            source.src= '/static/sounds/7.wav';
            audio.appendChild(source);
            audio.play();
            return;
        }

        var ogg = audio.canPlayType('audio/ogg; codecs="vorbis"');
        var mp3 = audio.canPlayType('audio/mpeg; codecs="mp3"');
        if (ogg == "probably" || ogg == "maybe") {
            source.type= 'audio/ogg';
            source.src= filename + '.ogg';
        } else if (mp3 == "probably" || mp3 == "maybe") {
            source.type= 'audio/mpeg';
            source.src= filename + '.mp3';
        }

        if (source.src.length > 1) {
            audio.appendChild(source);
            audio.play();
        }
    };
}
PartyCtrl.$inject = ['$scope', '$routeParams', '$timeout', 'RyyppyAPI'];


function PartyAdminCtrl($scope, $routeParams, RyyppyAPI) {


    $scope.active = "admin";

    RyyppyAPI.getParty($routeParams.partyId, function (data) {
        $scope.party = data;
    });
    RyyppyAPI.getPartyParticipants($routeParams.partyId, function (data) {
        $scope.participants = data;
    });

    $scope.email = null;
    $scope.name = null;
    $scope.sex = 'MALE';
    $scope.weight = null;

    $scope.addUser = function () {
        if ($scope.selectedUserTypeId == 1) {
            RyyppyAPI.addRegisteredUserToParty($routeParams.partyId, $scope.email, function (data) {
                alert($scope.email);
            });
        }
        else {
            var guest = { name: $scope.name, sex: $scope.sex, weight: $scope.weight };
            RyyppyAPI.addGuestToParty($routeParams.partyId, guest, function (data) {
                alert($scope.name + $scope.sex + $scope.weight);
            });
        }
    };

    $scope.removeUser = function (participant) {
        RyyppyAPI.removeUser($routeParams.partyId, participant, function (data) {
            alert($scope.email);
        });
    };

    $scope.userTypes = [
        {TypeId: 1, TypeName: 'Rekisteröitynyt käyttäjä'},
        {TypeId: 2, TypeName: 'Vieras'}
    ];
    $scope.selectedUserTypeId = 1;
}
PartyAdminCtrl.$inject = ['$scope', '$routeParams', 'RyyppyAPI'];