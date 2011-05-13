function UserButtonGrid(target) {
    this.target = target;
    this.userButtons = [];
    this.users = undefined;
}

UserButtonGrid.prototype.empty = function() {
    // TODO: Use this.target instead of hardcoded ID
    $('#drinkers').html('');
}

UserButtonGrid.prototype.updateGrid = function() {
    if (this.users === undefined)
        return;

    this.empty();

    var layout = this.pivotLayoutIfNecessary(this.determineLayout(this.users.length));
    RyyppyNet.layout = layout;
    var width = "" + (1 / layout[0] * 100) + "%;";
    var height = "" + (1 / layout[1] * 100) + "%;";
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
            ub.onDrunk = onUserDrunk;
            ub.onDataLoaded = onButtonDataUpdated;
            this.userButtons.push(ub);

            $('#row' + i).append(newElement);
        }
    }

    this.updateButtons();
}

UserButtonGrid.prototype.updateButtons = function() {
    for (i in grid.userButtons) {
        var userButton = grid.userButtons[i];
        userButton.update();
    }
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
