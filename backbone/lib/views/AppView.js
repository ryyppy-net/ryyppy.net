(function () {
    "use start";

    window.AppView = Backbone.View.extend({
        className: 'appView',

        initialize: function () {
            this.addUserView = new window.AddUserView();
            this.$el.append(this.addUserView.el);

            this.drinkerListView = new window.DrinkerListView();
            this.$el.append(this.drinkerListView.el);
        }
    });
})();
