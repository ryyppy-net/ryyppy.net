var alcoholScale = 3;
var timeScale = 5 * 60 * 60 * 1000;

function dataLoaded(data, userId, width, height, callback) {
	var rows = data.split('\n');

        histories = [];

	for (var i = 1; i < rows.length; i++) {
            var row = rows[i];
            if (row.length < 1) continue;
            var columns = row.split(',');

            var history = {"timestamp": new Date(columns[0]), "alcohol": Number(columns[1])};
            histories.push(history);
	}
        
        getChartFromGoogle(histories, userId, width, height, callback);
}

function getGraph(userId, width, height, callback) {
	$.get(historyUrl.replace('_userid_', userId), function(data) { dataLoaded(data, userId, width, height, callback); } );
}

String.prototype.format = function() {
    var formatted = this;
    for (var i = 0; i < arguments.length; i++) {
        var regexp = new RegExp('\\{'+i+'\\}', 'gi');
        formatted = formatted.replace(regexp, arguments[i]);
    }
    return formatted;
};

function getChartFromGoogle(histories, userId, width, height, callback) {
	if (width < 10) width = 10;

	var url = 
"http://chart.apis.google.com/chart?chxr=0,0,{4}&chf=bg,s,65432100&chxs=0,110101,11.5,0,l,676767&chxt=y&chs={1}x{2}&cht=lxy&chds=0,{3},0,{4}&chd=t:{0}&chdlp=b&chg={5},{6},0,0&chco={7}&chls=1";

	var w = Math.min(width, 500);
	var h = Math.min(height, 500);
	var data = reformData(histories);
	
	if (data == null || data.length == 0)
		return;

	url = url.format(data, Math.floor(w), Math.floor(h), timeScale, alcoholScale, 100 / 5, 100 / alcoholScale, '000000');

	callback(url, userId);
}

function reformData(datas)
{
    /**
     *
     * uncomment this to have fixed time scale
	var values = [];
	for (var i = 0; i < datas.length; i++) {
            var data = datas[i];
            if (data.timestamp.getTime() >= new Date().getTime() - timeScale) {
                values.push(data);
            }
	}
     */
        var values = datas;
	
	if (values.length == 0) return "";
	values.sort(function(a, b) {return (a.timestamp < b.timestamp) ? -1 : (a.timestamp == b.timestamp) ? 0 : 1;});
	
        timeScale = Math.abs(values[0].timestamp.getTime() - values[values.length - 1].timestamp.getTime());
        
	var y = "";
	var x = "";
	
	for (var i in values)
	{
            var userHistory = values[i];
            y += String(Number(userHistory.alcohol).toFixed(2)).replace(",", ".") + ",";

            var minutes = "" + Number(timeScale - (new Date().getTime() - userHistory.timestamp.getTime())).toFixed(2);
            x += ("" + minutes).replace(",", ".") + ",";

	}
	return x.substr(0, x.length - 1) + '|' + y.substr(0, y.length - 1);
}
