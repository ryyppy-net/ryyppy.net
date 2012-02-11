DrinkerList = Backbone.Collection.extend({
    model: Drinker,

    initialize: function () {
        console.log('DrinkerList initialized');
    }
});