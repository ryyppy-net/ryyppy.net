var drinksUrl = '/API/users/_userid_/drinks';

function UserHistoryGraph(user, element) {
    var that = this;
    this.series = [];
    this.element = element;
    this.options = {
        crosshair: { mode: "x" },
        grid: { hoverable: true, autoHighlight: false },
        yaxis: { min: 0, show:true},
        xaxis: { mode: "time", timeformat: "%y-%0m-%0d", minTickSize: [1, "day"], show:true },
        lines: { show: false },
        points: { show: false },
        bars: { show: true, barWidth: 24 * 60 * 60 * 1000, align: "center" }
    };
    this.updateLegendTimeout = null;
    this.latestPosition = null;
    this.plot = null;
    this.user = user;
    
    this.element.bind("plothover",  function (event, pos, item) {
        that.latestPosition = pos;
        if (!that.updateLegendTimeout)
            that.updateLegendTimeout = setTimeout(function() { that.updateLegend(); }, 50);
    });

    this.update = function() {
        this.render();

        $.get(drinksUrl.replace('_userid_', this.user.id), function(data) { that.gotData(data); } );
    }
    
    this.render = function() {
        this.plot = $.plot(this.element, this.series, this.options);
    }

    this.gotData = function(data) {
        var rows = data.split('\n');
        var histories = [];

        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');

            //var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();
            if (columns[1] == 0) columns[1] = -0.1;
            var history = [columns[0], Number(columns[1])];
            histories.push(history);
        }
        var newname = '66.66.6666 = 66';
        this.series = [{label: newname, data: histories}];

        $("#"+ this.element.attr('id') +" .legendLabel").each(function () {
            // fix the widths so they don't jump around
            $(this).css('width', $(this).width());
        });
        this.render();

        if (this.latestPosition == null) {
            this.latestPosition = {x: Number(histories[histories.length - 1][0]), y: 1 };
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
            for (j = 0; j < series.data.length; ++j)
                if (parseInt(series.data[j][0], 0) + 12 * 60 * 60 * 1000 > pos.x)
                    break;

            var d = new Date(pos.x + 12 * 60 * 60 * 1000);
            var s = d.getDate() + "." + (d.getMonth() + 1) + "." + d.getFullYear();
            $("#"+ this.element.attr('id') +" .legendLabel").eq(i).text(s + ": " + parseInt(series.data[j][1]));
        }
    }
}