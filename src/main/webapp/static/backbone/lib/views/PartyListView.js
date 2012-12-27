(function () {
    "use strict";

    var Ryyppy = window.Ryyppy;

    window.PartyListView = Backbone.View.extend({
        className: 'partyListView',

        initialize: function () {
            Ryyppy.parties.on('add', this.addOne, this);
        },

        addOne: function (party) {
            var partyView = new PartyView({ model: party });
            this.$el.append(partyView.el);
        }
    });
})();