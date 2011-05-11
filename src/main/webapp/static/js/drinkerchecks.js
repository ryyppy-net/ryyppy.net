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

function checkEmail(email, exclude) {
    $("#emailCorrect").html("<img src='/static/images/loading.gif' alt='loading...' />");
    if (email === exclude) {
        $("#emailCorrect").html("<img src='/static/images/yes.png' alt='email is ok' />").removeClass("error");
        return;
    }
    $.get('checkEmail?email=' + email, function(data) {
        if (data === '1')
            $("#emailCorrect").html("<img style='width:20px; height:20px;' src='/static/images/yes.png' alt='email is ok' />").removeClass("error");
        else
            $("#emailCorrect").html("<img style='width:20px; height:20px;' src='/static/images/no.png' alt='email is used' />").addClass("error");
        checkDrinkerFields();
    });
}

function getIdByEmail(email, partyId) {
    $("#emailCorrect").html("<img src='/static/images/loading.gif' alt='loading...' />");
    $.get('getUserByEmail?email=' + email + "&partyId=" + partyId, function(data) {
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