function checkDrinkerFields(checkEmail) {
    var success = true;
    
    var drinkerName = $('#drinkerName').val();
    if (drinkerName.length == 0)
        success = false;
    
    var drinkerWeight = $('#drinkerWeight').val();
    if (drinkerWeight.length == 0 || drinkerWeight != parseFloat(drinkerWeight))
        success = false;
    
    if (checkEmail && $("#emailCorrect").hasClass("error"))
        success = false;
    
    var button = $("#submitButton");
    button.prop("disabled", success ? "" : "disabled");
}

// Both lookups below fire one request per keystroke, and jQuery gives no
// ordering guarantee on the replies: under load the answer for a half-typed
// address routinely lands *after* the answer for the fully-typed one and
// overwrites it. Since a half-typed address is neither a valid nor a known
// one, the late reply marks the field wrong and disables the submit button,
// and nothing is left to re-check it - the form is stuck until the user
// types again (see #127). Stamp every request and let only the newest reply
// touch the UI.
var latestEmailCheck = 0;
var latestUserLookup = 0;

function checkEmail(email, exclude) {
    var sequence = ++latestEmailCheck;

    $("#emailCorrect").html("<img src='/static/images/loading.gif' alt='loading...' />");
    if (email === exclude) {
        $("#emailCorrect").html("<img src='/static/images/yes.png' alt='email is ok' />").removeClass("error");
        return;
    }
    $.get('checkEmail?email=' + email, function(data) {
        if (sequence !== latestEmailCheck) return;

        if (data === '1')
            $("#emailCorrect").html("<img style='width:20px; height:20px;' src='/static/images/yes.png' alt='email is ok' />").removeClass("error");
        else
            $("#emailCorrect").html("<img style='width:20px; height:20px;' src='/static/images/no.png' alt='email is used' />").addClass("error");
        checkDrinkerFields();
    });
}

function getIdByEmail(email, partyId) {
    var sequence = ++latestUserLookup;

    $("#emailCorrect").html("<img src='/static/images/loading.gif' alt='loading...' />");
    $.get('getUserByEmail?email=' + email + "&partyId=" + partyId, function(data) {
        if (sequence !== latestUserLookup) return;

        if (data !== '0') {
            $("#emailCorrect").html("<img style='width:20px; height:20px;' src='/static/images/yes.png' alt='user found' />").removeClass("error");
            $('#userId').val(data);
            $('#linkUserButton').prop("disabled", "");
        }
        else {
            $("#emailCorrect").html("<img style='width:20px; height:20px;' src='/static/images/no.png' alt='user not found' />").addClass("error");
            $('#linkUserButton').prop("disabled", "disabled");
        }
    });
}