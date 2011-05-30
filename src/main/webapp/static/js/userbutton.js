var partyHistoryUrl = '/API/parties/_partyid_/get-history';

function PartyHost(partyId) {
    var that = this;
    this.partyId = partyId;
    this.users = [];

    this.loadData = function() {
        $.get(partyHistoryUrl.replace('_partyid_', this.partyId), function(data) {that.historyLoaded(data);} );
    }
    
    this.historyLoaded = function(data) {
        var rows = data.split('\n');
        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');
            
            var userId = columns[0];

            if (this.users[userId] == undefined) {
                this.users[userId] = new User(userId);
            }
            
            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();
            var history = [Number(columns[1]) + timezoneoffset, Number(columns[2])];
            
            var user = this.users[userId];
            user.history.push(history);
        }
    }

    this.getUserHistoryFunction = function(userId) {
        var user = this.users[userId];
        return function() {
            return (user != undefined) ? user.history : undefined;
        };
    }
}


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
        xaxis: {mode: "time", timeformat: "%H:%M", show:true}
    };
    
    this.element = element;

    this.element.click(function () {that.buttonClick();} );
    
    this.buildHtml = function() {
        this.element.attr('id', 'user' + this.userId);
        this.element.css('background-color', color);

        var div = $('<div>');
        div.attr('id', 'info' + userId);
        this.element.append(div);
        this.setTexts(getMessage('loading'), 0, 0, 0);
    }

    this.setMaxY = function(max) {
        var old = this.graphOptions.yaxis.max;
        if (old != max) {
            this.graphOptions.yaxis.max = max;
            this.renderGraph();
        }
    }

    this.update = function() {
        RyyppyAPI.getUserData(this.userId, function(data) {
            that.dataLoaded(data);
        });
        RyyppyAPI.getUserHistory(this.userId, function(data) {
            that.historyLoaded(data);
        });
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
        div.append($('<span class="name"></span>').text(name));
        div.append($('<br />'));
        div.append($('<span class="details"></span>').text(Number(alcohol).toFixed(2)+'\u2030'));
        div.append($('<br />'));

        var click_me_msg = getMessage('click_me');
        var portion_msg = getMessage('portion');
        var portions_msg = getMessage('portions');
        var idle_msg = getMessage('idle');
        
        if (drinks == 0) {
            div.append($('<span class="details"></span>').text(click_me_msg));
        } else {
            if (drinks == 1)
                div.append($('<span class="details"></span>').text(drinks + portion_msg));
            else
                div.append($('<span class="details"></span>').text(drinks + portions_msg));

            div.append($('<br />'));
            div.append($('<span class="details"></span>').text(idle_msg + formatTime(idletime)));
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

            var history = [Number(columns[0]) + timezoneoffset, Number(columns[1])];
            histories.push(history);
        }

        var series = {data: histories, color: 'rgb(0, 0, 0)'};
        this.series = [series];

        this.renderGraph();
    }

    this.renderGraph = function() {
        if (this.element.height() < 150) return;
        if (this.element.width() < 200) return;
        
        var newElement = $('#graph' + this.userId);
        if (newElement.length == 0) {
            newElement = $('<div>');
            newElement.attr('id', 'graph' + this.userId);
            newElement.attr('class', 'graph');
            newElement.css('position', 'absolute');

            this.element.append(newElement);

            function onResize() {
                var width = parseFloat(that.element.css('width')) * 0.98;
                var height = parseFloat(that.element.css('height')) * 0.95;
                var top = getPositionTop(that.element.get(0)) + (parseFloat(that.element.css('height')) - height) / 2;
                var left = getPositionLeft(that.element.get(0)) + (parseFloat(that.element.css('width')) - width) / 2;

                var newElement = $('#graph' + that.userId);
                newElement.css('left', left);
                newElement.css('top', top);
                newElement.css('width', width);
                newElement.css('height', height);
            }
            this.element.resize(onResize);
            onResize();
        }

        if (this.series == null)
            return;

        $.plot(newElement, this.series, this.graphOptions);
    }

    this.buttonClick = function() {
        if (this.clicked)
            return;

        this.clicked = true;
        RyyppyAPI.addDrinkToUser(
            this.userId,
            this.showAdding,
            function() {alert(getMessage('drink_add_failed'));}
        );
    }
    
    this.showAdding = function(data) {
        var drinkUndone = false;
        var drinkId = data;

        var undoData = {UserId: that.userId, AddingDrinkMessage: getMessage('drink_added'), UndoMessage: getMessage('cancel_drink')};

        $.get('/static/templates/undoDrink.html', function(template) {
            var undoDiv = $.tmpl(template, undoData);
            undoDiv.appendTo('#user' + that.userId);
            fitElementOnAnother(undoDiv, that.element);
            
            var i = 0;
            
            setInterval(function() {
                i++;
                $("#progressbar").progressbar({ value: i });
            }, 50);

            undoDiv.fadeIn(500, function() {
                var timeoutId = setTimeout(function() {
                    that.fadeAndRemove(undoDiv);
                    that.enableButton();

                    if (!drinkUndone) {
                        that.update();
                        if (that.onDrunk) {
                           that.onDrunk(that.userId);
                        }

                        playSound();
                    }
                }, 5000);
                
                $('#portionSize').live('click', function() {
                    clearTimeout(timeoutId);
                });
                
                var undoButton = $('#undoButton' + that.userId);
                undoButton.live('click', function() {
                    clearTimeout(timeoutId);
                    
                    RyyppyAPI.removeDrinkFromUser(that.userId, drinkId);
                    drinkUndone = true;
                    undoButton.text(getMessage('drink_was_canceled'))
                              .css('background-color', 'red');
                    
                    setTimeout(function() {
                        that.fadeAndRemove(undoDiv);
                        that.enableButton();
                    }, 2000);
                });
                
                var editButton = $('#editButton' + that.userId);
                editButton.live('click', function() {
                    clearTimeout(timeoutId);
                    editButton.css('background-color', 'green');
                    
                    $.get('/static/templates/editDrink.html', function(template) {
                        var editDiv = $.tmpl(template, undoData);
                        editDiv.appendTo('#user' + that.userId);
                        fitElementOnAnother(editDiv, that.element);
                        undoDiv.hide();
                        editDiv.show();
                        
                        $('#acceptButton' + that.userId).live('click', function() {
                            editButton.css('background-color', 'black');
                            undoDiv.show();
                            editDiv.remove();
                        });
                    });
                });
            });
        });
    }
    
    this.fadeAndRemove = function(element) {
        element.fadeOut(500, function() {
            element.remove();
        });
    }
    
    this.disableButton = function() {
        this.clicked = true;
    }
    
    this.enableButton = function() {
        this.clicked = false;
    }
    
    this.buildHtml();
}

function formatTime(time) {
    var days = Math.floor(time / (60 * 60 * 24));
    time -= days * (60 * 60 * 24);
    var hours = Math.floor(time / (60 * 60));
    time -= hours * (60 * 60);
    var minutes = Math.floor(time / 60);

    var string = "";
    string += (days > 0) ? String(days) + " d " : "";
    string += (hours > 0) ? String(hours) + " h " : "";
    string += (minutes > 0) ? String(minutes) + " min " : "";

    if (string.length == 0) string += "0 min";

    return string;
}
