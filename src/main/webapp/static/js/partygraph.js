var datas = [];
var placeholder;
var placeid;
var options = {
    lines: { show: true },
    crosshair: { mode: "x" },
    grid: { hoverable: true, autoHighlight: false },
    yaxis: { min: 0 },
    xaxis: { mode: "time", timeformat: "%H:%M" }
};
var plot;
var updateLegendTimeout = null;
var latestPosition = null;

function render(place, persons) {
    placeid = place;
    placeholder = $('#' + placeid);
    placeholder.bind("plothover",  function (event, pos, item) {
        latestPosition = pos;
        if (!updateLegendTimeout)
            updateLegendTimeout = setTimeout(updateLegend, 50);
    });
    
    plot = $.plot(placeholder, datas, options);
    
    for (i in persons) {
        var person = persons[i];
        getData(person);
    }
}

function getData(person) {
    var url = person[1];
    var name = person[0];
    $.get(url, function(data) { gotData(name, data) });
}

function gotData(name, data) {
    var rows = data.split('\n');
    histories = [];

    for (var i = 1; i < rows.length; i++) {
        var row = rows[i];
        if (row.length == 0) continue;
        var columns = row.split(',');
        
        // todo fix hack
        timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();
        
        var history = [new Date(columns[0]).getTime() + timezoneoffset, Number(columns[1])];
        histories.push(history);
    }
    name = name + ' = 0.00';
    parsed = {label: name, data: histories};
    datas.push(parsed);
    
    $("#"+ placeid +" .legendLabel").each(function () {
        // fix the widths so they don't jump around
        $(this).css('width', $(this).width());
    });
    plot = $.plot(placeholder, datas, options);
}

function updateLegend() {
    updateLegendTimeout = null;

    var pos = latestPosition;

    var axes = plot.getAxes();
    if (pos.x < axes.xaxis.min || pos.x > axes.xaxis.max ||
        pos.y < axes.yaxis.min || pos.y > axes.yaxis.max)
        return;

    var i, j, dataset = plot.getData();
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

        $("#"+ placeid +" .legendLabel").eq(i).text(series.label.replace(/=.*/, "= " + y.toFixed(2)));
    }
}
