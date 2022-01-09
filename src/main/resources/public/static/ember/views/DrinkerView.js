DrinkerView = Backbone.View.extend({
    className: 'drinker',

    events: {
        'click': 'drink'
    },

    initialize: function () {
        this.template = _.template($('#drinker-template').html());
        this.render();

        this.model.on('change', this.render, this);
    },

    drink: function (event) {
        this.model.drink();
    },

    render: function () {
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }
});