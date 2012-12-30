"use strict";

function ProfileSettingsCtrl($scope, $location, RyyppyAPI, Notify) {
    $scope.active = 'profile-settings';

    RyyppyAPI.getProfile(function (data) {
        $scope.profile = data;

        $scope.name = data.name;
        $scope.email = data.email;
        $scope.sex = data.sex;
        $scope.weight = -12345;
    });

    $scope.updateProfile = function () {
        var profile = {
            name: $scope.name,
            email: $scope.email,
            sex: $scope.sex,
            weight: $scope.weight
        };

        if (profile.weight == -12345)
            profile.weight = $scope.profile.weight;

        RyyppyAPI.updateProfile(profile, function () {
            Notify.success("Tiedot päivitetty!", "Uudet asetukset tallennettu.");
            $location.path("/");
        });
    };
}

ProfileSettingsCtrl.$inject = ['$scope', '$location', 'RyyppyAPI', 'Notify'];