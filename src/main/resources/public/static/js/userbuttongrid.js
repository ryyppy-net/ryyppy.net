function UserButtonGrid(target) {
    this.target = target;
    this.userButtons = [];
    this.users = undefined;
    this.onUserDrunk = undefined;
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