function checkDrinkerFields() {
    var success = true;
    
    var drinkerName = $('#drinkerName').val();
    if (drinkerName.length == 0)
        success = false;
    
    var drinkerWeight = $('#drinkerWeight').val();
    if (drinkerWeight.length == 0 || drinkerWeight != parseFloat(drinkerWeight))
        success = false;
    
    var button = $("#submitButton");
    button.attr("disabled", success ? "" : "disabled");
}
