DrinkerRepository = {
    getCurrentUserId: function (callback) {
        $.getJSON('/API/v2/users', function (data) {
            if (typeof callback === 'function') {
                callback(data.users[0]);
            }
        });
    },

    getCurrentUser: function (callback) {
        $.getJSON('/API/v2/profile/', function (data) {
            if (typeof callback === 'function') {
                callback(data);
            }
        });
    },

    addDrinkToSelf: function (callback) {
        $.ajax({
            'type': 'POST',
            url: '/API/v2/parties/412/participants/411/drinks',
            data: {},
            success: function () {
                alert('success');
            },
            error: function (jqXHR, textStatus, errorThrown) {
                alert(textStatus);
            }
        });
    }
};