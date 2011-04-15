var historyUrl = '/API/users/_userid_/show-history';

function GroupGraph(users, element) {
    this.datas = [];
    this.element = element;
    this.options = {
        lines: { show: true },
        crosshair: { mode: "x" },
        grid: { hoverable: true, autoHighlight: false },
        yaxis: { min: 0, max:5 },
        xaxis: { mode: "time", timeformat: "%H:%M" }
    };
    this.updateLegendTimeout = null;
    this.latestPosition = null;
    this.plot = null;
    this.users = users;
    
    var fuckthis = this;
    this.element.bind("plothover",  function (event, pos, item) {
        fuckthis.latestPosition = pos;
        if (!fuckthis.updateLegendTimeout)
            fuckthis.updateLegendTimeout = setTimeout(function() { fuckthis.updateLegend(); }, 50);
    });

    this.update = function() {
        this.render();
        var fuckthis = this;
        
        for (var i in this.users) {
            var user = this.users[i];
            this.getData(user);
        }
    }
    
    this.getData = function(user) {
        $.get(historyUrl.replace('_userid_', user.id), function(data) { fuckthis.gotData(user, data); } );
    }
    
    this.render = function() {
        this.plot = $.plot(this.element, this.datas, this.options);
    }

    this.gotData = function(user, data) {
        var rows = data.split('\n');
        var histories = [];

        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');

            var timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();

            var history = [new Date(columns[0]).getTime() + timezoneoffset, Number(columns[1])];
            histories.push(history);
        }
        var newname = user.name + ' = 0.00';
        var parsed = {userid: user.id, label: newname, data: histories};
        var found = false;
        for (var j in this.datas) {
            var d = this.datas[j];
            if (d.userid === user.id) {
                found = true;
                this.datas.splice(j, 1, parsed);
                break;
            }
        }
        if (!found)
            this.datas.push(parsed);
        $("#"+ this.element.attr('id') +" .legendLabel").each(function () {
            // fix the widths so they don't jump around
            $(this).css('width', $(this).width());
        });
        this.render();
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
                if (series.data[j][0] > pos.x)
                    break;

            // now interpolate
            var y, p1 = series.data[j - 1], p2 = series.data[j];
            if (p1 == null)
                y = p2[1];
            else if (p2 == null)
                y = p1[1];
            else
                y = p1[1] + (p2[1] - p1[1]) * (pos.x - p1[0]) / (p2[0] - p1[0]);

            $("#"+ this.element.attr('id') +" .legendLabel").eq(i).text(series.label.replace(/=.*/, "= " + y.toFixed(2)));
        }
    }
}