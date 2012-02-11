DrinkerList = Backbone.Collection.extend({
    model: Person,

    initialize: function () {
        console.log('DrinkerList initialized');
    }
});