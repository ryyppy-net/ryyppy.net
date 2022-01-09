$(document).ready(function() {
    repaint();
    sendTimezone();
});

$(window).resize(function() {
    repaint();
});

function sendTimezone() {
    $.get("timezone/" + new Date().getTimezoneOffset());
}

function repaint() {
    var windowWidth = $(window).width();
    var bestWidth = Math.min(600, windowWidth - 20);
    $("#logoContainer").width(bestWidth);
    $(".login").width(bestWidth);
    $("#youtube").width(bestWidth);
    $("#youtube").height(bestWidth / (640 / 390));
}