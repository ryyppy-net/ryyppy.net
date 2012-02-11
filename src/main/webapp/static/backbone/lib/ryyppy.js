/**
 * Main program for Ryyppy.net
 */
(function () {
    "use strict";

    window.Ryyppy = {
        drinkers: undefined,

        start: function () {
            console.log('Ryyppy.net starting...');
            this.drinkers = new DrinkerList;
            new window.AppView({ el: 'body' });
        }
    };
})();