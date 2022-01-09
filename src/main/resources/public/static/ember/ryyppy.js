/**
 * Main program for Ryyppy.net
 */
(function () {
    "use strict";

    window.RyyppyRouter = Backbone.Router.extend({
        routes: {
            'help': 'help',
            '*other': 'defaultRoute'
        },

        defaultRoute: function () {
            console.log('default called');
        },

        help: function () {
            console.log('help called');
        }
    });

    var router = new RyyppyRouter();
    Backbone.history.start();

    window.Ryyppy = {
        user: undefined,
        drinkers: undefined,
        parties: undefined,

        start: function () {
            console.log('Ryyppy.net starting...');

            this.initializeUserData();
            this.initializePartyData();

            this.drinkers = new DrinkerList;
            this.parties = new PartyList;

            new window.AppView({ el: 'body' });
        },

        initializeUserData: function () {
            if (!localStorage.getItem('currentUserId')) {
                DrinkerRepository.getCurrentUserId(function (data) {
                    localStorage.setItem('currentUserId', data);
                });
            }
        },

        initializePartyData: function () {
            var that = this;

            PartyRepository.getParties(function (data) {
                _.each(data, function (partyJson) {
                    var party = new Party(partyJson);
                    that.parties.add(party);
                });
            });
        }
    };
})();