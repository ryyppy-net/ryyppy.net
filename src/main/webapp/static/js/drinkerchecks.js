function openAddDrinkerDialog() {
    $('#addDrinkerDialog').css('display', 'block')
                            .css('left', '50px')
                            .css('top', '50px');
}

function closeAddDrinkerDialog() {
    $('#addDrinkerDialog').css('display', 'none');
}

function checkDrinkerName() {
    var drinkerName = $('#drinkerName').val();
    for (var i in users) {
        if ( users[i].name == drinkerName ) {
            $('#drinkerName').css('background-color', 'red');
            alert(drinkerName + ' on jo käytössä! Valitse toinen nimi.')
            $('#drinkerName').focus();
            return;
        }
    }

    $('#drinkerName').css('background-color', 'white');
}
