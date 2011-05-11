var RyyppyNet = {
    needsRefreshing: false,
    users: [],
    inProgress: [],
    graph: null,
    graphInterval: null,
    graphVisible: false,
    updateInterval: 2 * 60 * 100,
    layout: []
};

var repaintAllowed = false;

$(document).ready(function() {
    repaintAllowed = true;
    repaint();
    initializeButtons();
    
    forceRefresh();
    
    // if resized, refresh
    setInterval(function() {if (RyyppyNet.needsRefreshing === true) forceRefresh();}, 1000);
    
    // update data every two minutes
    setInterval(function() {
        RyyppyAPI.getPartyData(partyId, function() { updateGrid });
    }, RyyppyNet.updateInterval);
});

$(window).resize(function() {
    repaint();
    var tmp = pivotLayoutIfNecessary(determineLayout(RyyppyNet.users.length));
    if (RyyppyNet.layout.length != tmp.length || RyyppyNet.layout[0] != tmp[0] || RyyppyNet.layout[1] != tmp[1])
        RyyppyNet.needsRefreshing = true;
});

function initializeButtons() {
    $('#graphButtonLink').click(function() {
        toggleDialog($('#graphDialog'), graphDialogOpened, graphDialogClosed);
    });

    $('#addDrinkerButtonLink').click(function() {
        toggleJQUIDialog($('#addDrinkerDialog'));
    });

    $('#kickDrinkerButtonLink').click(function() {
        toggleJQUIDialog($('#kickDrinkerDialog'));
    });

    $('#closeGraphDialogButton').click(function() {
        closeDialog($('#graphDialog'));
    });
}

function updateGroupGraph() {
    if (RyyppyNet.graph != null && RyyppyNet.graphVisible)
        RyyppyNet.graph.update();
}

function graphDialogClosed() {
    RyyppyNet.graphVisible = false;
    if (RyyppyNet.graphInterval)
        RyyppyNet.graphInterval = clearInterval(RyyppyNet.graphInterval);
}

function repaint() {
    if (!repaintAllowed) return;
    
    repaintAllowed = false;
    
    var windowWidth = $(window).width();
    var bestWidth = Math.min(600, windowWidth - 20);
    $("#addDrinkerDialog").width(bestWidth);

    $("#addDrinkerDialog").css('top', Math.max($("#addDrinkerDialog").css('top'), 0));
    
    $("#drinkers").height($(window).height() - $("#topic").height() - 20);
    $(".party").width($("#body").width() - 10 + 'px');
    
    repaintAllowed = true;
}

function forceRefresh() {
    RyyppyNet.needsRefreshing = false;
    RyyppyAPI.getPartyData(partyId, function(data) {
        grid.createAndFillGrid(data);
    });
}

function determineLayout(n) {
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

function pivotLayoutIfNecessary(layout) {
    var layout_aspect = layout[0] < layout[1];
    var window_aspect = $(window).width() < $(window).height();
    if (layout_aspect != window_aspect) {
        return [layout[1], layout[0]];
    }

    return layout;
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
    var newdata = parseData(data);
    
    if (!areSame(newdata, RyyppyNet.users)) {
        forceRefresh();
        return;
    }

    updateButtons();
}

function onUserDrunk() {
    updateGroupGraph();
}

function onButtonDataUpdated() {
    var max = 0;

    for (var i in grid.userButtons) {
        var userButton = grid.userButtons[i];
        if (userButton.series == null) continue;
        for (var j in userButton.series[0].data) {
            var d = userButton.series[0].data[j];
            var a = d[1];
            if (Number(a) >= Number(max))
                max = a;
        }
    }

    max = Math.floor(max) + 1;

    for (i in grid.userButtons) {
        userButton = grid.userButtons[i];
        userButton.setMaxY(max);
    }
}

function updateButtons() {
    for (i in grid.userButtons) {
        var userButton = grid.userButtons[i];
        userButton.update();
    }
}

function parseData(data) {
    var users = [];
    $(data).find('user').each(function() {
        var part = $(this);
        var user = {id: part.find('id').text(), alcohol: part.find('alcoholInPromilles').text()};

        users.push(user);
    });
    return users;
}

function UserButtonGrid(target) {
    this.target = target;
    this.userButtons = [];
}

UserButtonGrid.prototype.empty = function() {
    $(this.target).html('');
}

UserButtonGrid.prototype.createAndFillGrid = function(data) {
    RyyppyNet.users = parseData(data);

    var layout = pivotLayoutIfNecessary(determineLayout(RyyppyNet.users.length));
    RyyppyNet.layout = layout;
    var width = "" + (1 / layout[0] * 100) + "%;";
    var height = "" + (1 / layout[1] * 100) + "%;";
    for (var i = 0; i < layout[1]; i++) {
        $('#drinkers').append('<tr style="height:'+ height +'" id="row' + i + '"></tr>');
        for (var j = 0; j < layout[0]; j++) {
            var colorIndex = i*layout[0] + j;
            if (colorIndex >= RyyppyNet.users.length) continue;

            var newElement = $('<td>');
            newElement.addClass('userButton');
            newElement.addClass('roundedCornersBordered');
            newElement.attr("width", width);
            var user = RyyppyNet.users[colorIndex].id;
            var ub = new UserButton(user, newElement, getColorAtIndex(colorIndex));
            ub.onDrunk = onUserDrunk;
            ub.onDataLoaded = onButtonDataUpdated;
            this.userButtons.push(ub);

            $('#row' + i).append(newElement);
        }
    }

    updateButtons();
}
