function RyyppyAPI() {
    this.getUserDrinks = function(userId, callback) {
        $.get("/API/users/{0}/show-drinks".format(userId), callback);
    }

    this.removeDrinkFromUser = function(userId, drinkId, callback) {
        $.get('/API/users/{0}/remove-drink/{1}'.format(userId, drinkId), callback);
    }
}

var RyyppyAPI = new RyyppyAPI();

function fixTheFuckingCss() {
    $("#drinkers").height($(window).height() - $("#topic").height() - 20);
    
    $(".party").width($("#body").width() - 10 + 'px');

    addToolTips();
}

function addToolTips() {
    $(".headerButtonA").tooltip({
	delay: 500,
	showURL: false
    });
}

function toggleDialog(dialog, opened, closed) {
    if (dialog.css("display") == 'none')
        openDialog(dialog, opened);
    else
        closeDialog(dialog, closed);
}

function openDialog(dialog, opened) {
    var left = Math.floor(($(window).width() - dialog.width()) / 2);
    var top = Math.floor(($(window).height() - dialog.height()) / 3);

    dialog.css('left', left).css('top', top);
    dialog.show(300, opened);
}

function closeDialog(dialog, closed) {
    dialog.hide(300, closed);
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
    var audio = document.createElement("audio");
    var source = document.createElement('source');
    var filename = "/static/sounds/" + Math.floor(Math.random() * 7 + 1);

    if (!audio.canPlayType) return; // no html5 audio support
    
    if (navigator.userAgent.indexOf("Opera M") !== -1) { // stupid buggy opera mobile
        source.type= 'audio/wav';
        source.src= '/static/sounds/7.wav';
        audio.appendChild(source);
        audio.play();
        return;
    } 

    var ogg = audio.canPlayType('audio/ogg; codecs="vorbis"');
    var mp3 = audio.canPlayType('audio/mpeg; codecs="mp3"');
    if (ogg == "probably" || ogg == "maybe") {
        source.type= 'audio/ogg';
        source.src= filename + '.ogg';
    } else if (mp3 == "probably" || mp3 == "maybe") {
        source.type= 'audio/mpeg';
        source.src= filename + '.mp3';
    }

    if (source.src.length > 1) {
        audio.appendChild(source);
        audio.play();
    }
}

function getPositionLeft(el){
    var pL = 0;
    while(el) {pL += el.offsetLeft;el = el.offsetParent;}
    return pL;
}

function getPositionTop(el){
    var pT = 0;
    while(el) {pT += el.offsetTop;el = el.offsetParent;}
    return pT;
}

function getMessage( code )
{
    var msg = MESSAGES[getCookie('org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE')][code];
    if ( !msg )
        return 'No found';
    else 
        return msg;
}

var MESSAGES = new Array();
MESSAGES['en'] = new Array();
MESSAGES['fi'] = new Array();

MESSAGES['en']['click_me'] = 'Click here to drink';
MESSAGES['en']['portion'] = ' portion';
MESSAGES['en']['portions'] = ' portions';
MESSAGES['en']['idle'] = 'Idle ';
MESSAGES['en']['cancel_drink'] = 'Cancel drink';
MESSAGES['en']['loading'] = 'Loading...';
MESSAGES['en']['drink_added'] = "Adding a drink...";

MESSAGES['fi']['click_me'] = 'Paina tästä juodaksesi';
MESSAGES['fi']['portion'] = ' annos';
MESSAGES['fi']['portions'] = ' annosta';
MESSAGES['fi']['idle'] = 'Juomatta ';
MESSAGES['fi']['cancel_drink'] = 'Peru juoma';
MESSAGES['fi']['loading'] = 'Ladataan...';
MESSAGES['fi']['drink_added'] = "Juomaa lisätään...";

function getCookie( cookie_name )
{
    var i, name, value;
    var cookie_array = document.cookie.split(";");
    for ( i = 0; i < cookie_array.length; i++ )
    {
        name = cookie_array[i].substr( 0, cookie_array[i].indexOf("=") );
        value = cookie_array[i].substr( cookie_array[i].indexOf("=") + 1 );
        name = name.replace( /^\s+|\s+$/g, "" ); 
        if ( name == cookie_name )
            return unescape(value);
    }
    return 'fi';
}
