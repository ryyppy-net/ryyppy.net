'use strict';

/* Controllers */


function UserCtrl($scope, $http, RyyppyAPI) {
    RyyppyAPI.getProfile(function (data) {
        $scope.profile = data;
    });

    RyyppyAPI.getParties(function (data) {
        $scope.parties = data;
    });
}
UserCtrl.$inject = ['$scope', '$http', 'RyyppyAPI'];


function PartyCtrl($scope, $http, $routeParams, $timeout) {
    var self = this;
    $scope.active = "party";

    $http.get("/API/v2/parties/" + $routeParams.partyId).success(function (data) {
        $scope.party = data;
    });

    (function tick() {
        $http.get("/API/v2/parties/" + $routeParams.partyId + "/participants").success(function (data) {
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
        $http.post("/API/v2/parties/" + $routeParams.partyId + "/participants/" + participant.id + "/drinks", {"volume": 0.33, "alcohol": 0.5, "timestamp": null}).success(function (data) {
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
PartyCtrl.$inject = ['$scope', '$http', '$routeParams', '$timeout'];


function PartyAdminCtrl($scope, $http, $routeParams) {
    $http.defaults.headers.post["Content-Type"] = "application/x-www-form-urlencoded";

    $scope.active = "admin";

    $http.get("/API/v2/parties/" + $routeParams.partyId).success(function (data) {
        $scope.party = data;
    });
    $http.get("/API/v2/parties/" + $routeParams.partyId + "/participants").success(function (data) {
        $scope.participants = data;
    });

    $scope.email = null;
    $scope.name = null;
    $scope.sex = 'MALE';
    $scope.weight = null;

    $scope.addUser = function () {
        if ($scope.selectedUserTypeId == 1) {
            $http.post("/API/v2/parties/" + $routeParams.partyId + "/participants", $.param({
                email: $scope.email
            })).success(function (data) {
                alert($scope.email);
            });
        }
        else {
            $http.post("/API/v2/parties/" + $routeParams.partyId + "/participants", $.param({
                name: $scope.name, sex: $scope.sex, weight: $scope.weight
            })).success(function (data) {
                alert($scope.name + $scope.sex + $scope.weight);
            });
        }
    };

    $scope.removeUser = function (participant) {
        $http.delete("/API/v2/parties/" + $routeParams.partyId + "/participants/" + participant.id).success(function (data) {
            alert($scope.email);
        });
    };

    $scope.userTypes = [
        {TypeId: 1, TypeName: 'Rekisteröitynyt käyttäjä'},
        {TypeId: 2, TypeName: 'Vieras'}
    ];
    $scope.selectedUserTypeId = 1;
}
PartyAdminCtrl.$inject = ['$scope', '$http', '$routeParams'];