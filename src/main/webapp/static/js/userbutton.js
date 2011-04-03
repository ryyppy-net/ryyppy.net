var addDrinkUrl = '/API/users/_userid_/add-drink';
var historyUrl = '/API/users/_userid_/show-history';
var userUrl = '/API/users/_userid_/';

function UserButton(userId, element, color) {
    this.alcoholScale = 3;
    this.timeScale = 5 * 60 * 60 * 1000;
    this.userId = userId;
    this.clicked = false;
    
    this.graphOptions = {
        crosshair: {mode: null},
        yaxis: {min: 0, max: 5},
        xaxis: {mode: "time", timeformat: "%H:%M"}
    };
    
    this.element = element;

    var fuckthis = this;
    this.element.click(function () {fuckthis.buttonClick();} );
    
    this.buildHtml = function() {
        this.element.attr('id', 'user' + this.userId);
        this.element.css('background-color', color);

        var div = $('<div>');
        div.attr('id', 'info' + userId);
        this.element.append(div);
        this.setTexts("Loading", 0, 0, 0);
    }

    this.update = function() {
        // fucking javascript, plz fix this
        var fuckthis = this;
        
        $.get(userUrl.replace('_userid_', this.userId), function(data) {fuckthis.dataLoaded(data);} );
        $.get(historyUrl.replace('_userid_', this.userId), function(data) {fuckthis.historyLoaded(data);} );
    }

    this.dataLoaded = function(data) {
        var xml = $(data);
        var name = xml.find('name').text();
        var alcohol = xml.find('alcoholInPromilles').text();
        var drinks = xml.find('totalDrinks').text();
        var idletime = new Date().getTime() - new Date(xml.find('lastDrink').text()).getTime();

        this.setTexts(name, alcohol, drinks, idletime);
    }

    this.setTexts = function(name, alcohol, drinks, idletime) {
        var div = $('#info' + userId);
        
        var html = '<span class="name">' + name + '</span><br />'
        html += '<span class="details">' + Number(alcohol).toFixed(2) + ' \u2030<br />';
        html += drinks + " drinks <br /> idle: " + String(Math.floor(idletime / 1000 / 60)) + " min</span>";

        div.html(html);
    }

    this.historyLoaded = function(data) {
        var histories = [];

        var rows = data.split('\n');
        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');

            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();

            var history = [new Date(columns[0]).getTime() + timezoneoffset, Number(columns[1])];
            histories.push(history);
        }

        var series = {data: histories, color: 'rgb(0, 0, 0)'};

        this.renderGraph(series);
    }

    this.renderGraph = function(series) {
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
        
        $.plot(newElement, [series], this.graphOptions);
    }

    this.buttonClick = function() {
        if (this.clicked)
            return;
        this.clicked = true;
        
        var fuckthis = this;

        $.get(addDrinkUrl.replace('_userid_', this.userId), function() {
            fuckthis.update();
            fuckthis.element.fadeTo('slow', 1.0, function() {fuckthis.clicked = false;});
           // fuckthis.css('border-style', 'outset');
        });

        // this.element.css('border-style', 'inset');
        this.element.fadeTo('slow', 0.5);
        playSound();
    }

    this.buildHtml();
}
