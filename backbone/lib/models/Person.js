/**
 * Person can participate in parties and drink.
 */
Person = Backbone.Model.extend({
    defaults: {
        drinks: 0,
        name: 'Anonymous'
    },

    initialize: function () {
        console.log('Person initialized');
    }
});