var drinksUrl = '/API/users/_userid_/drinks';

function UserHistoryGraph(user, element) {
    var that = this;
    this.series = [];
    this.element = element;
    this.options = {
        crosshair: {mode: "x"},
        grid: {hoverable: true, autoHighlight: false},
        yaxis: {min: 0, show:true},
        xaxis: {mode: "time", timeformat: "%y-%0m-%0d", minTickSize: [1, "day"], show:true},
        lines: {show: false},
        points: {show: false},
        bars: {show: true, barWidth: 24 * 60 * 60 * 1000, align: "center"}
    };
    this.updateLegendTimeout = null;
    this.latestPosition = null;
    this.plot = null;
    this.user = user;
    
    this.element.bind("plothover",  function (event, pos, item) {
        that.latestPosition = pos;
        if (!that.updateLegendTimeout)
            that.updateLegendTimeout = setTimeout(function() {that.updateLegend();}, 50);
    });

    this.update = function() {
        this.render();

        $.get(drinksUrl.replace('_userid_', this.user.id), function(data) {that.gotData(data);} );
    }
    
    this.render = function() {
        var newElement = $('#historyGraph2');
        if (newElement.length == 0) {
            newElement = $('<div>');
            newElement.attr('id', 'historyGraph2');
            newElement.attr('class', 'graph');
            newElement.css('position', 'absolute');

            this.element.append(newElement);

            function onResize() {
                var width = parseFloat(that.element.css('width')) * 0.98;
                var height = parseFloat(that.element.css('height')) * 0.95;
                var top = getPositionTop(that.element.get(0)) + (parseFloat(that.element.css('height')) - height) / 2;
                var left = getPositionLeft(that.element.get(0)) + (parseFloat(that.element.css('width')) - width) / 2;

                var newElement = $('#historyGraph2');
                newElement.css('left', left);
                newElement.css('top', top);
                newElement.css('width', width);
                newElement.css('height', height);
            }
            this.element.resize(onResize);
            onResize();
        }

        if (this.series == null)
            return;

        this.plot = $.plot(newElement, this.series, this.options);
    }

    this.gotData = function(data) {
        var rows = data.split('\n');
        var histories = [];

        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');

            var timestamp = Number(columns[0]);
            var drinks = Number(columns[1]);

            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();
            timestamp = timestamp + timezoneoffset;
            if (drinks == 0) drinks = -0.1;
            var history = [timestamp, drinks];
            histories.push(history);
        }

        if (histories[histories.length - 1][0] - histories[0][0] < 30 * 24 * 60 * 60 * 1000) {
            histories.splice(0, 0, [histories[0][0] - 30 * 24 * 60 * 60 * 1000, -0.1]);
        }

        var newname = '66.66.6666 = 66 ' + getMessage('portions');
        this.series = [{color:"rgb(0, 0, 0)", label: newname, data: histories}];

        $("#"+ this.element.attr('id') +" .legendLabel").each(function () {
            // fix the widths so they don't jump around
            $(this).css('width', $(this).width());
        });
        this.render();

        if (this.latestPosition == null) {
            this.latestPosition = {x: Number(histories[histories.length - 1][0]), y: 1};
            this.updateLegend();
        }
    }

    this.updateLegend = function() {
        this.updateLegendTimeout = null;

        var pos = this.latestPosition;

        var axes = this.plot.getAxes();
        if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max ||
            pos.y < axes.yaxis.min || pos.y > axes.yaxis.max)
            return;

        var i, j, dataset = this.plot.getData();
        for (i = 0; i < dataset.length; ++i) {
            var series = dataset[i];

            // find the nearest points, x-wise
            var found = false;
            for (j = 0; j < series.data.length; ++j) {
                if (parseInt(series.data[j][0], 0) + 12 * 60 * 60 * 1000 > pos.x && parseInt(series.data[j][0], 0) - 12 * 60 * 60 * 1000 <= pos.x) {
                    found = true;
                    break;
                }
            }

            if (found) {
                var d = new Date(pos.x + 12 * 60 * 60 * 1000);
                var s = d.getDate() + "." + (d.getMonth() + 1) + "." + d.getFullYear();
                $("#"+ this.element.attr('id') +" .legendLabel").eq(i).text(s + ": " + Math.round(series.data[j][1]) + getMessage('portions'));
            } else {
                var d2 = new Date(pos.x + 12 * 60 * 60 * 1000);
                var s2 = d2.getDate() + "." + (d2.getMonth() + 1) + "." + d2.getFullYear();
                $("#"+ this.element.attr('id') +" .legendLabel").eq(i).text(s2 + ": 0 " + getMessage('portions'));
            }
        }
    }
}