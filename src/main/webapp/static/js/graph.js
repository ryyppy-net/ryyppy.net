var alcoholScale = 3;
var timeScale = 300;

var personhistories = [];

function generateChart(name) {
	alert(personhistories[name]);

	var data = personhistories[name];
}

function loaded(data, callback) {
	var rows = data.split('\n');

	for (var i = 1; i < rows.length; i++) {
		var row = rows[i];
		
		var columns = row.split(',');
		
		var person = {"timestamp": new Date(columns[0]), "name": columns[1], "alcohol": Number(columns[2]), "drinks": columns[3]};
		if (!personhistories[person.name])
			personhistories[person.name] = []
		personhistories[person.name].push(person);
	}

	callback();
}

function initialize(callback) {
	$.get(historyUrl, function(data) { loaded(data, callback); } );
}

String.prototype.format = function() {
    var formatted = this;
    for (var i = 0; i < arguments.length; i++) {
        var regexp = new RegExp('\\{'+i+'\\}', 'gi');
        formatted = formatted.replace(regexp, arguments[i]);
    }
    return formatted;
};

function getChart(name, width, height, callback) {
	if (width < 10) width = 10;

	var url = 
"http://chart.apis.google.com/chart?chxr=0,0,{4}&chf=bg,s,65432100&chxs=0,110101,11.5,0,l,676767&chxt=y&chs={1}x{2}&cht=lxy&chds=0,{3},0,{4}&chd=t:{0}&chdlp=b&chg={5},{6},0,0&chco={7}&chls=1";

	var w = Math.min(width, 500);
	var h = Math.min(height, 500);
	var asd = getDataFromHistories(name);
	
	if (asd == null || asd.length == 0)
		return;

	url = url.format(asd, Math.floor(w), Math.floor(h), timeScale, alcoholScale, 100 / (timeScale / 60), 100 / alcoholScale, '000000');

	callback(url, name);
}

function getDataFromHistories(name)
{
	var datas = personhistories[name];
	var values = [];
	for (var i = 0; i < datas.length; i++) {
		var data = datas[i];
		if (data.timestamp.getTime() >= new Date().getTime() - timeScale * 60 * 1000) {
			values.push(data);
		}
	}
	
	if (values.length == 0) return "";
	values.sort(function(a, b) { return (a.timestamp < b.timestamp) ? -1 : (a.timestamp == b.timestamp) ? 0 : 1;});
	
	var y = "";
	var x = "";
	
	for (var i in values)
	{
		var personHistory = values[i];
		y += String(Number(personHistory.alcohol).toFixed(2)).replace(",", ".") + ",";

		var minutes = "" + Number(timeScale - (new Date().getTime() - personHistory.timestamp.getTime()) / (1000 * 60)).toFixed(2);
		x += ("" + minutes).replace(",", ".") + ",";

	}
	return x.substr(0, x.length - 1) + '|' + y.substr(0, y.length - 1);
}
