function arraysEqual(a, b) {
    if (a.length !== b.length) return false;
    for (var i = 0; i < a.length; i++) {
        if (a[i] !== b[i]) return false;
    }
    return true;
}

function UserButtonGrid(target) {
    this.target = target;
    this.userButtons = [];
    this.users = undefined;
    this.onUserDrunk = undefined;
    this.renderedUserIds = null;
    this.renderedLayout = null;
}

UserButtonGrid.prototype.empty = function() {
    // TODO: Use this.target instead of hardcoded ID
    $('#drinkers').html('');
}

UserButtonGrid.prototype.updateGrid = function() {
    if (this.users === undefined)
        return;

    var layout = this.pivotLayoutIfNecessary(this.determineLayout(this.users.length));
    var userIds = this.users.map(function(user) { return user.id; });

    // repaint() (and so updateGrid()) runs on every window resize and every
    // popup dialog open, not just when the participant list actually
    // changes. Rebuilding the whole grid from scratch every time destroys
    // and recreates each drinker's <td> and its .resize() binding (and its
    // sparkline flot plot), and that churn corrupts the bundled legacy
    // resize-event plugin bindings enough to crash jquery.flot.resize on a
    // later dialog open (see #80). Skip the rebuild when nothing changed.
    if (this.renderedUserIds != null &&
        arraysEqual(this.renderedUserIds, userIds) &&
        arraysEqual(this.renderedLayout, layout)) {
        this.updateButtons();
        return;
    }

    this.empty();

    RyyppyNet.layout = layout;
    var width = "" + (1 / layout[0] * 100) + "%;";
    var height = "" + (1 / layout[1] * 100) + "%;";
    this.userButtons = [];
    for (var i = 0; i < layout[1]; i++) {
        $('#drinkers').append('<tr style="height:'+ height +'" id="row' + i + '"></tr>');
        for (var j = 0; j < layout[0]; j++) {
            var colorIndex = i*layout[0] + j;
            if (colorIndex >= this.users.length) continue;

            var newElement = $('<td>');
            newElement.addClass('userButton');
            newElement.addClass('roundedCornersBordered');
            newElement.attr("width", width);
            var user = this.users[colorIndex].id;
            var ub = new UserButton(user, newElement, getColorAtIndex(colorIndex));
            ub.onDrunk = this.onUserDrunk;
            this.userButtons.push(ub);

            $('#row' + i).append(newElement);
        }
    }

    this.renderedUserIds = userIds;
    this.renderedLayout = layout;

    this.updateButtons();
}

UserButtonGrid.prototype.updateButtons = function() {
    this.userButtons.map(function(userButton) { userButton.update(); });
    this.resetUserButtonMaximumPromilles();
}

UserButtonGrid.prototype.determineLayout = function(n) {
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

UserButtonGrid.prototype.pivotLayoutIfNecessary = function(layout) {
    var layout_aspect = layout[0] < layout[1];
    var window_aspect = $(window).width() < $(window).height();
    if (layout_aspect != window_aspect) {
        return [layout[1], layout[0]];
    }

    return layout;
}

UserButtonGrid.prototype.resetUserButtonMaximumPromilles = function() {
    var max = 0;

    for (var i in this.userButtons) {
        var userButton = this.userButtons[i];
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
        userButton = this.userButtons[i];
        userButton.setMaxY(max);
    }
}