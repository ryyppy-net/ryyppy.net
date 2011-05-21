var RyyppyNet = {
    inProgress: [],
    graph: null,
    graphInterval: null,
    graphVisible: false,
    updateInterval: 2 * 60 * 1000
};

var repaintAllowed = false;

$(document).ready(function() {
    repaintAllowed = true;
    repaint();
    initializeButtons();
    
    // update data every two minutes
    setInterval(function() {
        partyHost.update();
    }, RyyppyNet.updateInterval);
});

$(window).resize(function() {
    repaint();
});

function initializeButtons() {
    $('#graphButtonLink').click(function() {
        toggleJQUIDialog($('#fooDialog'));
    });

    $('#addDrinkerButtonLink').click(function() {
        toggleJQUIDialog($('#addDrinkerDialog'));
    });

    $('#kickDrinkerButtonLink').click(function() {
        toggleJQUIDialog($('#kickDrinkerDialog'));
    });
}

function updateGroupGraph() {
    if (RyyppyNet.graph != null && RyyppyNet.graphVisible)
        RyyppyNet.graph.update();
}

function graphDialogOpened() {
    RyyppyNet.graphVisible = true;
    var element = $('#groupGraph');
    var dialog = $('#fooDialog');

    repaint();

    var height = dialog.height();
    var width = dialog.width();
    
    element.width(dialog.width()).height(parseInt(dialog.height(), 0) - 10);

    if (RyyppyNet.graph == null) {
        RyyppyNet.graph = new GroupGraph(partyHost.users, element);
    }

    RyyppyNet.graph.options.legend = {position:'nw'};
    updateGroupGraph();
    if (!RyyppyNet.graphInterval)
        RyyppyNet.graphInterval = setInterval(updateGroupGraph, RyyppyNet.updateInterval);
}

function graphDialogClosed() {
    RyyppyNet.graphVisible = false;
    if (RyyppyNet.graphInterval)
        RyyppyNet.graphInterval = clearInterval(RyyppyNet.graphInterval);
}

function repaint() {
    if (!repaintAllowed) return;
    
    repaintAllowed = false;
    
    grid.updateGrid();
    $("#drinkers").height($(window).height() - $("#topic").height() - 30);
    $("#drinkers").width($(window).width() - 20);

    $(".party").width($("#body").width() - 10 + 'px');

    var bestSize = calculateBestDialogSize();
    resizePopupDialogs(bestSize);

    repaintAllowed = true;
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


function pageUpdate() {
    $('#topic').text(partyHost.name);
    grid.users = partyHost.users;
    grid.updateGrid();
    
    if (RyyppyNet.graph !== null)
        RyyppyNet.graph.users = partyHost.users;
}



function PartyHost(partyId) {
    this.partyId = partyId;
    this.name = "";
    this.users = [];
    this.onUpdate = undefined;
    
    this.update();
}

PartyHost.prototype.parseData = function(data) {
    this.parseUserData(data);
    this.parsePartyData(data);
}

PartyHost.prototype.parsePartyData = function(data) {
    // TODO: Ugly hack
    this.name = $($(data).find('name')[0]).text();
}

PartyHost.prototype.parseUserData = function(data) {
    var users = [];

    $(data).find('user').each(function() {
        var part = $(this);

        var user = new User();
        user.id = part.find('id').text();
        user.promilles = part.find('alcoholInPromilles').text();
        user.name = part.find('name').text();

        users.push(user);
    });

    this.users = users;
}

PartyHost.prototype.update = function() {
    var that = this;
    RyyppyAPI.getPartyData(partyId, function(data) {
        that.parseData(data);

        if (typeof that.onUpdate === 'function')
            that.onUpdate();
    });
}


function User() {
    this.id = -1;
    this.name = "";
    this.promilles = 0.0;
}
