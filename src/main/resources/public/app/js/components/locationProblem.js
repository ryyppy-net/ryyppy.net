(function (angular) {
    'use strict';

    /** What a page that needs the user's position shows when the browser cannot give it. */
    function LocationProblemCtrl($window) {
        this.host = $window.location.hostname;
    }

    LocationProblemCtrl.$inject = ['$window'];

    angular.module('ryyppy').component('locationProblem', {
        controller: LocationProblemCtrl,
        bindings: {
            // 'denied' or 'unavailable', as Geolocation.current() rejects with
            reason: '<',
            onRetry: '&'
        },
        template: `
    <div class="nearby-page-message" ng-if="$ctrl.reason === 'denied'">
        <svg class="nearby-page-message-icon" role="img" aria-label="Sijainti estetty" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><path d="M12 21.5s6.5-6.6 6.5-11.5a6.5 6.5 0 0 0-11.3-4.4M6 8.7C5.6 9.1 5.5 9.6 5.5 10c0 4.9 6.5 11.5 6.5 11.5"></path><path d="M3 3l18 18"></path></svg>
        <h1>Sijainti on estetty</h1>
        <p>Lähellä olevia bileitä ei voi etsiä ilman sijaintia. Salli sijainti {{ $ctrl.host }}-sivustolle selaimen asetuksista ja yritä uudelleen.</p>
        <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.onRetry()">Yritä uudelleen</button>
        <p class="location-invite-small">Voit liittyä bileisiin myös kutsulinkillä.</p>
        <a class="btn btn-large" href="#!/">Takaisin etusivulle</a>
    </div>

    <div class="nearby-page-message" ng-if="$ctrl.reason === 'unavailable'">
        <svg class="nearby-page-message-icon" role="img" aria-label="Sijaintia ei löydy" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round"><path d="M12 21.5s6.5-6.6 6.5-11.5a6.5 6.5 0 0 0-13 0c0 4.9 6.5 11.5 6.5 11.5z"></path><path d="M10 9.2a2 2 0 1 1 2.8 1.8c-.5.3-.8.7-.8 1.3M12 14.6v.1"></path></svg>
        <h1>Sijaintia ei saatu</h1>
        <p>Laite ei löytänyt sijaintiasi. Tarkista, että sijaintipalvelut ovat päällä, tai siirry ulos ja yritä uudelleen.</p>
        <button type="button" class="btn btn-success btn-large" ng-click="$ctrl.onRetry()">Yritä uudelleen</button>
        <a class="btn btn-large" href="#!/">Takaisin etusivulle</a>
    </div>`
    });
})(angular);
