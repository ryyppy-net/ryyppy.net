(function () {
    "use strict";

    var Ryyppy = window.Ryyppy;

    window.AddUserView = Backbone.View.extend({
        className: 'addUserView',

        initialize: function() {
            this.template = _.template($("#search_template").html());
            this.render();
        },

        render: function() {
            this.$el.html(this.template);
        },

        events: {
            'click .add-user': 'doSearch'
        },

        doSearch: function (event) {
            var name = this.$el.find('input').first().val();
            var person = new Drinker({name: name});
            Ryyppy.drinkers.add(person);
        }
    });
})();