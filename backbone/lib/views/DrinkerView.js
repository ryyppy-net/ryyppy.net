DrinkerView = Backbone.View.extend({
    className: 'drinker',

    events: {
        'click': 'drink'
    },

    initialize: function () {
        this.template = _.template($('#drinker-template').html());
        this.render();
    },

    drink: function (event) {
        console.log('drinker ' + this.model.get('name') + ' drank');
    },

    render: function () {
        this.$el.html(this.template(this.model.toJSON()));
        return this;
    }
});