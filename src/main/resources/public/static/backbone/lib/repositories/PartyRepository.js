PartyRepository = {
    getParties: function (callback) {
        $.getJSON('/API/v2/parties/', function (data) {
            if (typeof callback === 'function') {
                callback(data);
            }
        });
    }
};