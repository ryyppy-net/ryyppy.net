function openAddDrinkerDialog() {
    var left = Math.floor(($("#body").width() - $("#addDrinkerDialog").width()) / 2);
    var top = Math.floor(($("#body").height() - $("#addDrinkerDialog").height()) / 3);

    $('#addDrinkerDialog').css('display', 'block')
                            .css('left', left)
                            .css('top', top);
}

function closeAddDrinkerDialog() {
    $('#addDrinkerDialog').css('display', 'none');
}

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
