$(function() {
    var App = App || {};


    App.models = App.models || {};




    var Drinker = Backbone.Model.extend({
        defaults: {
            id: -1,
            name: '',
            promilles: 0.0
        }
    });

    App.models.Drinker = Drinker || {};



    var DrinkerCollection = Backbone.Collection.extend({
        model: Drinker,
    });

    var Drinkers = new DrinkerCollection;


    App.addDrinker = function(name) {
        var newDrinker = new Drinker({ id: 1, name: name, promilles: 1.2 });
        Drinkers.add(newDrinker);
    };





    var DrinkerView = Backbone.View.extend({
        el: $('#drinkers'),

        drinkerTemplate: $('#drinkerTemplate'),

        render: function(drinker) {
            var foo = $('#drinkerTemplate');
            var source = foo.html();
            var template = Handlebars.compile(source);
            $('#drinkers').append(template(drinker));
        }
    });

    DrinkerList = new DrinkerView;

    Drinkers.bind('add', DrinkerList.render, this);




    var PartyView = Backbone.View.extend({
        el: $('#partyView'),


        initialize: function() {

        }
    });



    $('#addDrinker').live('submit', function(event) {
        event.preventDefault();
        App.addDrinker($('#name').val());
    });
});