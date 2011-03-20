var needsRefreshing = false;
var users;
var inProgress = [];

// entry point
$(document).ready(function() {
    forceRefresh();
    
    // if resized, refresh
    setInterval(function() {if (needsRefreshing == true) forceRefresh();}, 1000);
    
    // update data every minute
    setInterval(function() {get_data(updateGrid);}, 60 * 1000);
});

$(window).resize(function() {
    needsRefreshing = true;
});

// clamp i to 16-255
function fix(i) {if(i < 16) {return 16;}if(i > 255) {return 255;}return i;}

function vari() {
    var d = 500;
    var r = Math.random()*255;
    var g = Math.random()*255;
    var b = Math.random()*255;
    var sum = r+g+b;
    r /= (sum/d);
    g /= (sum/d);
    b /= (sum/d);
    return '#'+parseInt(fix(r)).toString(16)+parseInt(fix(g)).toString(16)+parseInt(fix(b)).toString(16);
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
            forceRefresh();
            return;
        }
    }
    
    updateGraphs();
}

function getUserHtml(d) {
    return '<span class="name">' + d.name + '</span><br /><span class="details">' + Number(d.alcohol).toFixed(2) + ' ‰<br />' + d.drinks + " drinks <br /> idle: " + String(Math.floor(d.idletime / 1000 / 60)) + " min</span>";
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
            newElement.css('background-color', vari());
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
        getGraph(user.id, w, h, picLoaded);
    }
}

function picLoaded(url, userId) {
    $('#' + userId).css('background-image', 'url(\'' + url + '\')');
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

function fix_the_fucking_css() {
    $("#drinkers").height($(window).height() - $("#topic").height() - 20);
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
