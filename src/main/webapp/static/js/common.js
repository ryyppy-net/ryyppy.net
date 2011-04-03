function fix_the_fucking_css() {
    $("#drinkers").height($(window).height() - $("#topic").height() - 20);
    
    $(".party").width($("#body").width() - 10 + 'px');
}

function toggleAddDrinkerDialog() {
    if ($('#addDrinkerDialog').css("display") == 'none')
        openAddDrinkerDialog();
    else
        closeAddDrinkerDialog();
}

function openAddDrinkerDialog() {
    var left = Math.floor(($(window).width() - $("#addDrinkerDialog").width()) / 2);
    var top = Math.floor(($(window).height() - $("#addDrinkerDialog").height()) / 3);

    $('#addDrinkerDialog').css('left', left).css('top', top);
    $('#addDrinkerDialog').show(300);
}

function closeAddDrinkerDialog() {
    $('#addDrinkerDialog').hide(300);
}

var colors = [
  "#65E6A4",
  "#65A8E6",
  "#68E665",
  "#1DB466",
  "#A8E665",
  "#DD318A",
  "#E6E465",
  "#E66568",
  "#65E6E4",
  "#6568E6",
  "#31DD84",
  "#A465E6",
  "#B41D6B",
  "#E465E6",
  "#E6A465",
  "#E665A8",
];

function getColorAtIndex(i) {
    return colors[i % 16];
}

String.prototype.format = function() {
    var formatted = this;
    for (var i = 0; i < arguments.length; i++) {
        var regexp = new RegExp('\\{'+i+'\\}', 'gi');
        formatted = formatted.replace(regexp, arguments[i]);
    }
    return formatted;
};

function playSound() {
    var filename = "/static/sounds/" + Math.floor(Math.random() * 8 + 1) + ".wav.ogg";
    var snd = new Audio(filename);
    snd.play();
}

function getPositionLeft(el){
    var pL = 0;
    while(el) { pL += el.offsetLeft; el = el.offsetParent; }
    return pL;
}

function getPositionTop(el){
    var pT = 0;
    while(el) { pT += el.offsetTop; el = el.offsetParent; }
    return pT;
}
