/**
 * Person can participate in parties and drink.
 */
Drinker = Backbone.Model.extend({
    defaults: {
        totalDrinks: 0,
        name: 'Anonymous'
    },

    initialize: function () {
        console.log('Person initialized');
    },

    drink: function () {
        this.set('totalDrinks', this.get('totalDrinks') + 1);
        DrinkerRepository.addDrinkToSelf();
    }
});