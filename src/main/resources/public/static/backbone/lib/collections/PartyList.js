PartyList = Backbone.Collection.extend({
    model: Party,

    initialize: function () {
        console.log('PartyList initialized');
    }
});