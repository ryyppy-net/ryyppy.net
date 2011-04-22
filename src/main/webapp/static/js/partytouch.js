var needsRefreshing = false;
var users;
var inProgress = [];
var userButtons = [];

// entry point
$(document).ready(function() {
    forceRefresh();
    
    // if resized, refresh
    setInterval(function() {if (needsRefreshing == true) forceRefresh();}, 1000);
    
    // update data every two minutes
    setInterval(function() {get_party_data(updateGrid);}, updateInterval);
});

$(window).resize(function() {
    needsRefreshing = true;
});

function forceRefresh() {
    needsRefreshing = false;
    $('#drinkers').html('');
    get_party_data(createAndFillGrid);
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

function get_party_data(callback) {
    $.get(dataUrl, callback);
}

function areSame(list1, list2) {
    if (list1.length != list2.length) return false;
    
    for (var i in list1) {
        var found = false;
        for (var j in list2) {
            if (list1[i].id == list2[j].id) {
                found = true;
                break;
            }
        }
        if (!found) return false;
    }
    
    return true;
}

function updateGrid(data) {
    var newdata = parse_data(data);
    
    if (!areSame(newdata, users)) {
        forceRefresh();
        return;
    }

    updateButtons();
}

function onUserDrunk() {
    updateGroupGraph();
}

function createAndFillGrid(data) {
    $('#drinkers').html('');
    users = parse_data(data);
    
    var layout = determine_layout(users.length);
    layout = pivot_layout_if_necessary(layout);
    var width = "" + (1 / layout[0] * 100) + "%;";
    var height = "" + (1 / layout[1] * 100) + "%;";
    for (var i = 0; i < layout[1]; i++) {
        $('#drinkers').append('<tr style="height:'+ height +'" id="row' + i + '"></tr>');
        for (var j = 0; j < layout[0]; j++) {
            var colorIndex = i*layout[0] + j;
            if (colorIndex >= users.length) continue;
            
            var newElement = $('<td>');
            newElement.addClass('userButton');
            newElement.attr("width", width);
            var user = users[colorIndex].id;
            var ub = new UserButton(user, newElement, getColorAtIndex(colorIndex));
            ub.onDrunk = onUserDrunk;
            ub.onDataLoaded = onButtonDataUpdated;
            userButtons.push(ub);

            $('#row' + i).append(newElement);
        }
    }
    fixTheFuckingCss();
    
    updateButtons();
}

function onButtonDataUpdated() {
    var max = 0;

    for (var i in userButtons) {
        var userButton = userButtons[i];
        if (userButton.series == null) continue;
        for (var j in userButton.series[0].data) {
            var d = userButton.series[0].data[j];
            var a = d[1];
            if (Number(a) >= Number(max))
                max = a;
        }
    }

    max = Math.floor(max) + 1;

    for (i in userButtons) {
        userButton = userButtons[i];
        userButton.setMaxY(max);
    }
}

function updateButtons() {
    for (i in userButtons) {
        var userButton = userButtons[i];
        userButton.update();
    }
}

function parse_data(data) {
    var users = [];
    $(data).find('user').each(function() {
        var part = $(this);
        var user = {id: part.find('id').text(), alcohol: part.find('alcoholInPromilles').text()};

        users.push(user);
    });
    return users;
}
