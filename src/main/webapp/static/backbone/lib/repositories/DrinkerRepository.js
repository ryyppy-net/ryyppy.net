DrinkerRepository = {
    getCurrentUserId: function (callback) {
        $.getJSON('/API/v2/users', function (data) {
            if (typeof callback === 'function') {
                callback(data.users[0]);
            }
        });
    },

    getCurrentUser: function (callback) {
        $.getJSON('/API/v2/users/' + localStorage.getItem('currentUserId'), function (data) {
            if (typeof callback === 'function') {
                callback(data);
            }
        });
    }
};