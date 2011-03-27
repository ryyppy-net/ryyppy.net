var needsRefreshing = false;
var users;
var inProgress = [];

// entry point
$(document).ready(function() {
    forceRefresh();
    
    // if resized, refresh
    setInterval(function() {if (needsRefreshing == true) forceRefresh();}, 1000);
    
    // update data every two minutes
    setInterval(function() {get_data(updateGrid);}, 2 * 60 * 1000);
});

$(window).resize(function() {
    needsRefreshing = true;
});

// clamp i to 16-255
function fix(i) {if(i < 16) {return 16;}if(i > 255) {return 255;}return i;}

function getColorAtIndex(i) {
    return colors[i % 16];
}

function forceRefresh() {
    needsRefreshing = false;
    $('#drinkers').html('');
    get_data(createAndFillGrid);
}

function determine_layout(n) {
    var best = [0, 0];
    var initial = Math.ceil(Math.sqrt(n));
    var square_candidate = initial * initial;
    var other_candidate  = (initial - 1) * (initial + 1);

    if (Math.abs(square_candidate - n) < Math.abs(other_candidate - n)) {
        best = [initial, initial];
    } else {
        best = [initial - 1, initial + 1];
    }

    if ((best[0] * (best[1] - 1)) >= n) {
        best[1] = best[1] - 1;
    }

    return best;
}

function pivot_layout_if_necessary(layout) {
    var layout_aspect = layout[0] < layout[1];
    var window_aspect = $(window).width() < $(window).height();
    if (layout_aspect != window_aspect) {
        return [layout[1], layout[0]];
    }

    return layout;
}

// gets party data 
function get_data(callback) {
    $.get(dataUrl, callback);
}

function updateGrid(data) {
    var newdata = parse_data(data);

    for (var i in users) {
        var user = users[i];
        var found = false;

        for (var j in newdata) {
            var d = newdata[j];
            if (d.id == user.id) {
                $('#' + user.id).html(getUserHtml(d));
                found = true;
                break;
            }
        }
        
        if (!found) {
            // someone was removed
            forceRefresh();
            return;
        }
    }

    for (var j in newdata) {
        var d = newdata[j];
        var found = false;

        for (var i in users) {
            var user = users[i];
            if (d.id == user.id) {
                found = true;
                break;
            }
        }
        if (!found) {
            // someone was added
            forceRefresh();
            return;
        }
    }
    
    users = newdata;
    
    updateGraphs();
}

function getUserHtml(d) {
    var html = '<span class="name">' + d.name + '</span><br />'
    html += '<span class="details">' + Number(d.alcohol).toFixed(2) + ' ‰<br />' + d.drinks + " drinks <br /> idle: " + String(Math.floor(d.idletime / 1000 / 60)) + " min</span>";

    return html;
}

function createAndFillGrid(data) {
    $('#drinkers').html('');
    users = parse_data(data);

    var layout = determine_layout(users.length);
    layout = pivot_layout_if_necessary(layout);
    for (var i = 0; i < layout[1]; i++) {
        $('#drinkers').append('<tr id="row' + i + '"></tr>');
        for (var j = 0; j < layout[0]; j++) {
            var colorIndex = i*layout[0] + j;
            if (colorIndex >= users.length) continue;
            var newElement = $('<td>');
            var user = users[colorIndex];

            newElement.attr('id', user.id);
            newElement.html(getUserHtml(user));
            newElement.css('background-color', getColorAtIndex(colorIndex));
            newElement.click(function() {
                buttonClick(this);
            });
            $('#row' + i).append(newElement);
        }
    }

    fix_the_fucking_css();
    
    updateGraphs();
}

function updateGraphs() {
    for (var i in users) {
        var user = users[i];
        var w = $('#' + user.id).width();
        var h = $('#' + user.id).height();
        getGraph(user.id, picLoaded);
    }
}

var options = {
    lines: { show: true },
    yaxis: { min: 0, max: 5 },
    xaxis: { mode: "time", timeformat: "%H:%M" }
};

function getPositionLeft(This){
var el = This;var pL = 0;
while(el){pL+=el.offsetLeft;el=el.offsetParent;}
return pL;
}

function getPositionTop(This){
var el = This;var pT = 0;
while(el){pT+=el.offsetTop;el=el.offsetParent;}
return pT;
}

function picLoaded(data, userId) {
    var h = $('#' + userId);
    
    var newElement = $('#graph' + userId);
    if (newElement.length == 0) {
        newElement = $('<div>');
        newElement.attr('id', 'graph' + userId);
        newElement.css('position', 'absolute');
        
        var width = parseFloat(h.css('width')) * 0.9;
        var height = parseFloat(h.css('height')) * 0.9;
        var top = getPositionTop(h.get(0)) + (parseFloat(h.css('height')) - height) / 2;
        var left = getPositionLeft(h.get(0)) + (parseFloat(h.css('width')) - width) / 2;
        newElement.css('width', width);
        newElement.css('height', height);
        newElement.css('left', left);
        newElement.css('top', top);
        h.append(newElement);
    }
        
    $.plot(newElement, [data], options);
}

function buttonClick(sender) {
    if (inProgress[sender])
        return;
    inProgress[sender] = true;

    $.get(addDrinkUrl.replace('_userid_', sender.id), function() {
        get_data(updateGrid);
        $(sender).fadeTo('slow', 1.0);
        inProgress[sender] = false;
        $(sender).css('border-style', 'outset');
    });
    
    $(sender).css('border-style', 'inset');
    $(sender).fadeTo('slow', 0.5);
    playSound();
}

function parse_data(data) {
    var users = [];
    $(data).find('user').each(function() {
        var part = $(this);
        var user = {};
        user.name = part.find('name').text();
        user.alcohol = part.find('alcoholInPromilles').text();
        user.drinks = part.find('totalDrinks').text();
        user.id = part.find('id').text();
        user.idletime = new Date().getTime() - new Date(part.find('lastDrink').text()).getTime();

        users.push(user);
    });
    return users;
}

function playSound() {
    var filename = "/static/sounds/" + Math.floor(Math.random() * 8 + 1) + ".wav.ogg";
    var snd = new Audio(filename);
    snd.play();
}
