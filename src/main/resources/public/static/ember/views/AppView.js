(function () {
    "use start";

    window.AppView = Backbone.View.extend({
        className: 'appView',

        initialize: function () {
            var that = this;

            DrinkerRepository.getCurrentUser(function (data) {
                var model = new Drinker(data);
                that.currentUserView = new CurrentDrinkerView({ model: model });
                that.$el.append(that.currentUserView.el);
            });

            this.addUserView = new window.AddUserView();
            this.$el.append(this.addUserView.el);

            this.drinkerListView = new window.DrinkerListView();
            this.$el.append(this.drinkerListView.el);

            this.partyListView = new window.PartyListView();
            this.$el.append(this.partyListView.el);
        }
    });
})();
