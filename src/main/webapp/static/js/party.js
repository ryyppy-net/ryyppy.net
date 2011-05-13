var RyyppyNet = {
    needsRefreshing: false,
    users: [],
    inProgress: [],
    graph: null,
    graphInterval: null,
    graphVisible: false,
    updateInterval: 2 * 60 * 1000,
    layout: []
};

var repaintAllowed = false;

$(document).ready(function() {
    repaintAllowed = true;
    repaint();
    initializeButtons();
    
    forceRefresh();
    
    // if resized, refresh
    setInterval(function() {
        if (RyyppyNet.needsRefreshing === true) forceRefresh();
    }, 1000);
    
    // update data every two minutes
    setInterval(function() {
        RyyppyAPI.getPartyData(partyId, function() {
            updateGrid();
        });
    }, RyyppyNet.updateInterval);
});

$(window).resize(function() {
    repaint();
    var tmp = grid.pivotLayoutIfNecessary(grid.determineLayout(RyyppyNet.users.length));
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
    $("#addDrinkerDialog").dialog('option', 'width', bestWidth);
    $("#kickDrinkerDialog").dialog('option', 'width', bestWidth);

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

    grid.updateButtons();
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

function parseData(data) {
    var users = [];
    $(data).find('user').each(function() {
        var part = $(this);
        var user = {
            id: part.find('id').text(), 
            alcohol: part.find('alcoholInPromilles').text()
        };

        users.push(user);
    });
    return users;
}
