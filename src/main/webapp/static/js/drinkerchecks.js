function checkDrinkerFields() {
    var success = true;
    
    var drinkerName = $('#drinkerName').val();
    if (drinkerName.length == 0)
        success = false;
    
    var drinkerWeight = $('#drinkerWeight').val();
    if (drinkerWeight.length == 0 || drinkerWeight != parseFloat(drinkerWeight))
        success = false;
    
    if ($("#emailCorrect").hasClass("error"))
        success = false;
    
    var button = $("#submitButton");
    button.attr("disabled", success ? "" : "disabled");
}

function checkEmail(email) {
    $("#emailCorrect").css("display", "block");
    $("#emailCorrect").html("<img src='/static/images/loading.gif' alt='loading...' />").addClass("error");
    $.get('checkEmail?email=' + email, function(data) {
        if (data === '1')
            $("#emailCorrect").html("<img src='/static/images/yes.png' alt='email is ok' />").removeClass("error");
        else
            $("#emailCorrect").html("<img src='/static/images/no.png' alt='email is used' />");
        checkDrinkerFields();
    });
}

function getIdByEmail(email, partyId) {
    $("#emailCorrect").css("display", "block");
    $("#emailCorrect").html("<img src='/static/images/loading.gif' alt='loading...' />").addClass("error");
    $('#linkUserButton').attr("disabled", "disabled");
    $.get('getUserByEmail?email=' + email + "&partyId=" + partyId, function(data) {
        if (data !== '0') {
            $("#emailCorrect").html("<img src='/static/images/yes.png' alt='user found' />").removeClass("error");
            $('#userId').val(data);
            $('#linkUserButton').attr("disabled", "");
        }
        else
            $("#emailCorrect").html("<img src='/static/images/no.png' alt='user not found' />");
    });
}