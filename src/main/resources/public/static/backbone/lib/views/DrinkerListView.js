(function () {
    "use strict";

    var Ryyppy = window.Ryyppy;

    window.DrinkerListView = Backbone.View.extend({
        className: 'drinkerListView',

        initialize: function () {
            Ryyppy.drinkers.on('add', this.addOne, this);
        },

        addOne: function (drinker) {
            var drinkerView = new DrinkerView({ model: drinker });
            this.$el.append(drinkerView.el);
        }
    });
})();