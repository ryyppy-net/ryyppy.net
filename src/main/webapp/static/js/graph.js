var alcoholScale = 3;
var timeScale = 5 * 60 * 60 * 1000;

function dataLoaded(data, userId, callback) {
	var rows = data.split('\n');

        histories = [];

        for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length == 0) continue;
            var columns = row.split(',');

            timezoneoffset = -1 * 1000 * 60 * new Date().getTimezoneOffset();

            var history = [new Date(columns[0]).getTime() + timezoneoffset, Number(columns[1])];
            histories.push(history);
        }
        
        series = {data: histories, color: 'rgb(0, 0, 0)'};
        
        callback(series, userId);
}

function getGraph(userId, callback) {
	$.get(historyUrl.replace('_userid_', userId), function(data) { dataLoaded(data, userId, callback); } );
}

String.prototype.format = function() {
    var formatted = this;
    for (var i = 0; i < arguments.length; i++) {
        var regexp = new RegExp('\\{'+i+'\\}', 'gi');
        formatted = formatted.replace(regexp, arguments[i]);
    }
    return formatted;
};
