PartyView = Backbone.View.extend({
    className: 'party',

    initialize: function () {
        this.template = _.template($('#party-template').html());
        this.render();
    },

    render: function () {
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }
});