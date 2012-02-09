/**
 * Main program for Ryyppy.net
 */
(function () {
    "use strict";

    window.Ryyppy = function () {
        return {
            start: function () {
                console.log('Ryyppy.net started');
                var person = new Person();
                console.log(JSON.stringify(person.toJSON()));
            }
        }
    };
})();