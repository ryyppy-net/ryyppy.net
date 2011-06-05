function DrinkProgressBar(element) {
    this.element = element;
    this.progress = 0;
    this.updateIntervalId = undefined;
    this.time = 5000;
    
    return this;
}

DrinkProgressBar.prototype.start = function() {
    this.reset();
    this.updateIntervalId = setInterval(
        $.proxy(this.stepThrough, this),
        this.time / 100
    );
};

DrinkProgressBar.prototype.stepThrough = function() {
    if (this.progress > 100) {
        this.stop();
        return;
    }

    this.progress++;
    this.update();
}

DrinkProgressBar.prototype.stop = function() {
    clearInterval(this.updateIntervalId);
}

DrinkProgressBar.prototype.reset = function() {
    this.stop();
    this.progress = 0;
    this.update();
}

DrinkProgressBar.prototype.update = function() {
    this.element.progressbar({value: this.progress});
}

DrinkProgressBar.prototype.remove = function() {
    this.element.remove();
}




function UserButton(userId, element, color) {
    var that = this;

    this.userId = userId;
    this.element = element;
    this.color = color;

    this.alcoholScale = 3;
    this.timeScale = 5 * 60 * 60 * 1000;
    this.clicked = false;
    this.onDrunk = null;
    this.alcohol = 0;
    this.onDataLoaded = null;
    this.series = null;
    
    this.minimumWidth = 240;
    this.minimumHeight = 240;
    
    this.defaultPortionSize = 0.33;
    this.defaultPortionAlcoholPercentage = 0.047;
    
    this.selectedPortionSize = this.defaultPortionSize;
    this.selectedPortionAlcoholPercentage = this.defaultPortionAlcoholPercentage;
    
    this.progressBar = null;
    
    this.timeoutId = undefined;
    this.undoDiv = undefined;
    
    this.buttonElement;
    
    this.graphOptions = {
        crosshair: {mode: null},
        yaxis: {min: 0},
        xaxis: {mode: "time", timeformat: "%H:%M", show:true}
    };
    
    this.portionSizes = {
        '0.04': '4 cl',
        '0.08': '8 cl',
        '0.12': '12 cl',
        '0.16': '16 cl',
        '0.2': '0.2 l',
        '0.33': '0.33 l',
        '0.4': '0.4 l',
        '0.5': '0.5 l',
        '0.6': '0.6 l',
        '1.0': '1.0 l'
    };
    
    this.portionAlcoholPercentages = {
        '0.028': '2.8%',
        '0.047': '4.7%',
        '0.052': '5.2%',
        '0.055': '5.5%',
        '0.08': '8.0%',
        '0.12': '12.0%',
        '0.14': '14.0%',
        '0.22': '22.0%',
        '0.30': '30.0%',
        '0.32': '32.0%',
        '0.38': '38.0%',
        '0.40': '40.0%',
        '0.48': '48.0%',
        '0.80': '80.0%'
    };

    this.graphElement = undefined;
    
    this.buildHtml();
}


UserButton.prototype.buildHtml = function() {
    var buttonData = {
        'UserId': this.userId
    };

    $.get('/static/templates/userButton.html', $.proxy(function(template) {
        this.buttonElement = $.tmpl(template, buttonData);
        this.buttonElement.appendTo(this.element);
        this.initializeButton();
        
        this.graphElement = $('#graph' + this.userId);
        this.element.resize($.proxy(this.onResize, this));
        $.proxy(this.onResize, this)();
        this.renderGraph();
    }, this));
}

UserButton.prototype.initializeButton = function() {
    this.buttonElement.css('background-color', this.color);
    this.buttonElement.click($.proxy(this.buttonClick, this));
    this.setTexts(getMessage('loading'), 0, 0, 0);
}

UserButton.prototype.setMaxY = function(max) {
    var old = this.graphOptions.yaxis.max;
    if (old != max) {
        this.graphOptions.yaxis.max = max;
        this.renderGraph();
    }
}

UserButton.prototype.update = function() {
    RyyppyAPI.getUserData(this.userId, $.proxy(this.dataLoaded, this));
    RyyppyAPI.getUserHistory(this.userId, $.proxy(this.historyLoaded, this));
}

UserButton.prototype.dataLoaded = function(data) {
    var xml = $(data);
    this.name = xml.find('name').text();
    var alcohol = xml.find('alcoholInPromilles').text();
    var drinks = xml.find('totalDrinks').text();
    var idletime = xml.find('idle').text();

    this.alcohol = alcohol;

    this.setTexts(alcohol, drinks, idletime);
    if (this.onDataLoaded != null)
        this.onDataLoaded(data);

    $('#info' + this.userId).css('top', ($('#infoContainer' + this.userId).height() / 2) - ($('#info' + this.userId).height() / 2));
}

UserButton.prototype.setTexts = function(alcohol, drinks, idletime) {
    var div = $('#info' + this.userId);
    div.html('');
    div.append($('<span class="name"></span>').text(this.name));
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

UserButton.prototype.historyLoaded = function(data) {
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

UserButton.prototype.renderGraph = function() {
    if (this.graphElement == undefined)
        return;
    
    if (this.element.height() < 150 || this.element.width() < 200) {
        this.graphElement.hide();
        return;
    } 

    if (this.series == null)
        return;

    this.graphElement.show();
    $.plot(this.graphElement, this.series, this.graphOptions);
}


UserButton.prototype.onResize = function() {
    var width = parseFloat(this.element.css('width')) * 0.98;
    var height = parseFloat(this.element.css('height')) * 0.95;
    var top = getPositionTop(this.element.get(0)) + (parseFloat(this.element.css('height')) - height) / 2;
    var left = getPositionLeft(this.element.get(0)) + (parseFloat(this.element.css('width')) - width) / 2;

    this.graphElement.css('left', left);
    this.graphElement.css('top', top);
    this.graphElement.css('width', width);
    this.graphElement.css('height', height);
}

UserButton.prototype.buttonClick = function() {
    if (this.clicked)
        return;

    this.clicked = true;
    this.showAdding();
}

UserButton.prototype.addDrink = function() {
    RyyppyAPI.addDrinkToUser(
        this.userId,
        this.selectedPortionSize,
        this.selectedPortionAlcoholPercentage,
        $.proxy(function(data) {
            this.update();
            playSound();
        }, this),
        function() {
            alert(getMessage('drink_add_failed'));
        }
    );

    this.selectedPortionSize = this.defaultPortionSize;
    this.selectedPortionAlcoholPercentage = this.defaultPortionAlcoholPercentage;
}


UserButton.prototype.scheduleAddingDrink = function() {
    this.cancelAddingDrink();
    this.timeoutId = setTimeout($.proxy(function() {
        this.fadeAndRemove(this.undoDiv);
        this.enableButton();

        this.addDrink();
        this.progressBar.remove();
        if (this.onDrunk) {
           this.onDrunk(this.userId);
        }
    }, this), 5000);
}

UserButton.prototype.cancelAddingDrink = function() {
    clearTimeout(this.timeoutId);
}

UserButton.prototype.showAdding = function() {
    var that = this;
    
    var undoData = {
        UserId: that.userId,
        AddingDrinkMessage: getMessage('drink_added'),
        UndoMessage: getMessage('cancel_drink'),
        PortionSizeLabel: getMessage('portion_size'),
        PortionAlcoholPercentageLabel: getMessage('portion_alcohol_percentage'),
        EditDrinkLabel: getMessage('edit_drink'),
        AcceptLabel: getMessage('accept')
    };

    $.get('/static/templates/undoDrink.html', function(template) {
        that.undoDiv = $.tmpl(template, undoData);
        that.undoDiv.appendTo('#body');
        that.fitElementOnAnotherOrFullScreen(that.undoDiv, $('#user' + that.userId));

        that.updatePortionSizeAndAlcoholPercentage();

        that.progressBar = new DrinkProgressBar($("#progressbar" + that.userId));
        that.progressBar.start();

        that.undoDiv.fadeIn(500, function() {
            that.scheduleAddingDrink();

            var editButton = $('#editButton' + that.userId);
            editButton.click(function() {
                that.cancelAddingDrink();
                editButton.css('background-color', 'green');

                $.get('/static/templates/editDrink.html', function(template) {
                    var editDiv = $.tmpl(template, undoData);
                    editDiv.appendTo('#body');
                    that.fitElementOnAnotherOrFullScreen(editDiv, $('#user' + that.userId));

                    that.undoDiv.hide();
                    editDiv.show();

                    $('#acceptButton' + that.userId).click(function() {
                        that.selectedPortionSize = $('#portionSize' + that.userId).val();
                        that.selectedPortionAlcoholPercentage = $('#portionAlcoholPercentage' + that.userId).val();
                        that.addDrink();
                        that.undoDiv.remove();
                        that.fadeAndRemove(editDiv);
                        that.enableButton();
                    });
                });
            });

            var undoButton = $('#undoButton' + that.userId);
            undoButton.click(function() {
                that.cancelAddingDrink();
                editButton.unbind('click');
                that.progressBar.stop();

                undoButton.text(getMessage('drink_was_canceled'))
                          .css('background-color', 'red');

                setTimeout(function() {
                    that.fadeAndRemove(that.undoDiv);
                    that.progressBar.remove();
                    that.enableButton();
                }, 2000);
            });
        });
    });
}

UserButton.prototype.fadeAndRemove = function(element) {
    element.fadeOut(500, function() {
        element.remove();
    });
}

UserButton.prototype.disableButton = function() {
    this.clicked = true;
}

UserButton.prototype.enableButton = function() {
    this.clicked = false;
}

UserButton.prototype.fitElementOnAnotherOrFullScreen = function(element, another) {
    if (another.width() < this.minimumWidth || another.height() < this.minimumHeight) {
        element.css({
            'top': 5,
            'left': 5,
            'width': $(window).width() - 10,
            'height': $(window).height() - 10
        });
    } else {
        fitElementOnAnother(element, another);
    }
}

UserButton.prototype.updatePortionSizeAndAlcoholPercentage = function() {
    $('#userNameLabel' + this.userId).html(this.name);
    $("#portionLabel" + this.userId).html(this.portionSizes[this.selectedPortionSize] + ' @ ' + this.portionAlcoholPercentages[this.selectedPortionAlcoholPercentage]);
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
