function fix_the_fucking_css() {
    $("#drinkers").height($(window).height() - $("#topic").height() - 20);
    $("#addDrinkerButton").height($("#header").height() - 8);
    $("#addDrinkerButton").css("font-size", $("#addDrinkerButton").height() + 'px');
    $("#addDrinkerButton").width($("#addDrinkerButton").height());

    $("#goBack").height($("#header").height() - 8);
    $("#goBack").css("font-size", $("#goBack").height() + 'px');
    $("#goBack").width($("#goBack").height());

    $("#graphButton").height($("#header").height() - 8);
    $("#graphButton").css("font-size", $("#graphButton").height() + 'px');
    $("#graphButton").width($("#goBack").height());
    
    $(".party").width($("#body").width() - 10 + 'px');
}

function openPopupDialog() {
    var left = Math.floor(($(window).width() - $("#addDrinkerDialog").width()) / 2);
    var top = Math.floor(($(window).height() - $("#addDrinkerDialog").height()) / 3);

    $('#addDrinkerDialog').css('display', 'block')
                            .css('left', left)
                            .css('top', top);
}

function closeAddDrinkerDialog() {
    $('#addDrinkerDialog').css('display', 'none');
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
