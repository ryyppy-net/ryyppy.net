var addDrinkUrl = '/API/users/_userid_/add-drink';
var historyUrl = '/API/users/_userid_/show-history';
var userUrl = '/API/users/_userid_/';

function UserButton(userId, element, color) {
    var that = this;
    this.alcoholScale = 3;
    this.timeScale = 5 * 60 * 60 * 1000;
    this.userId = userId;
    this.clicked = false;
    this.onDrunk = null;
    this.alcohol = 0;
    this.onDataLoaded = null;
    this.series = null;
    
    this.graphOptions = {
        crosshair: {mode: null},
        yaxis: {min: 0},
        xaxis: {mode: "time", timeformat: "%H:%M"}
    };
    
    this.element = element;

    this.element.click(function () {that.buttonClick();} );
    
    this.buildHtml = function() {
        this.element.attr('id', 'user' + this.userId);
        this.element.css('background-color', color);

        var div = $('<div>');
        div.attr('id', 'info' + userId);
        this.element.append(div);
        this.setTexts("Ladataan", 0, 0, 0);
    }

    this.setMaxY = function(max) {
        var old = this.graphOptions.yaxis.max;
        if (old != max) {
            this.graphOptions.yaxis.max = max;
            this.renderGraph();
        }
    }

    this.update = function() {
        $.get(userUrl.replace('_userid_', this.userId), function(data) {that.dataLoaded(data);} );
        $.get(historyUrl.replace('_userid_', this.userId), function(data) {that.historyLoaded(data);} );
    }

    this.dataLoaded = function(data) {
        var xml = $(data);
        var name = xml.find('name').text();
        var alcohol = xml.find('alcoholInPromilles').text();
        var drinks = xml.find('totalDrinks').text();
        var idletime = xml.find('idle').text();

        this.alcohol = alcohol;

        this.setTexts(name, alcohol, drinks, idletime);
        if (this.onDataLoaded != null)
            this.onDataLoaded(data);
    }

    this.setTexts = function(name, alcohol, drinks, idletime) {
        var div = $('#info' + userId);
        div.html('');
        div.append($('<span class="name"></span>').text(name))
        div.append($('<br />'))
        div.append($('<span class="details"></span>').text(Number(alcohol).toFixed(2)+'\u2030'))
        div.append($('<br />'))

        if (drinks == 0) {
            div.append($('<span class="details"></span>').text('paina tästä juodaksesi'))
        } else {
            if (drinks == 1)
                div.append($('<span class="details"></span>').text(drinks + ' annos'))
            else
                div.append($('<span class="details"></span>').text(drinks + ' annosta'))

            div.append($('<br />'))
            div.append($('<span class="details"></span>').text("juomatta: " + String(Math.floor(idletime / 60)) + ' min'))
        }
    }

    this.historyLoaded = function(data) {
        var histories = [];

        var rows = data.split('\n');
        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');

            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();

            var history = [columns[0] + timezoneoffset, Number(columns[1])];
            histories.push(history);
        }

        var series = {data: histories, color: 'rgb(0, 0, 0)'};
        this.series = [series];

        this.renderGraph();
    }

    this.renderGraph = function() {
        var newElement = $('#graph' + this.userId);
        if (newElement.length == 0) {
            newElement = $('<div>');
            newElement.attr('id', 'graph' + this.userId);
            newElement.attr('class', 'graph');
            newElement.css('position', 'absolute');

            var width = parseFloat(this.element.css('width')) * 0.9;
            var height = parseFloat(this.element.css('height')) * 0.9;
            var top = getPositionTop(this.element.get(0)) + (parseFloat(this.element.css('height')) - height) / 2;
            var left = getPositionLeft(this.element.get(0)) + (parseFloat(this.element.css('width')) - width) / 2;
            newElement.css('width', width);
            newElement.css('height', height);
            newElement.css('left', left);
            newElement.css('top', top);
            
            this.element.append(newElement);
        }

        if (this.series == null)
            return;

        $.plot(newElement, this.series, this.graphOptions);
    }

    this.buttonClick = function() {
        if (this.clicked)
            return;
        this.clicked = true;
        
        $.get(addDrinkUrl.replace('_userid_', this.userId), function() {
            that.update();
            that.element.fadeTo('slow', 1.0, function() {that.clicked = false;});
            // that.css('border-style', 'outset');
            if (that.onDrunk)
               that.onDrunk(that.userId);
        });

        // this.element.css('border-style', 'inset');
        this.element.fadeTo('slow', 0.5);
        playSound();
    }
    
    this.buildHtml();
}
