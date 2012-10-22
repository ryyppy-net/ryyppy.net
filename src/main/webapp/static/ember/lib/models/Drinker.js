/**
 * Drinker can participate in parties and drink.
 */

Drinker = Ember.Object.extend({
    drinks: 0,
    name: 'Anonymous',

    initialize: function () {
        console.log('Person initialized');
    },

    drink: function () {
        this.set('drinks', this.get('drinks') + 1);
        //DrinkerRepository.addDrinkToSelf();
    }
});