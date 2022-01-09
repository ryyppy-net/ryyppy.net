String.prototype.format = function() {
    var formatted = this;
    for (var i = 0; i < arguments.length; i++) {
        var regexp = new RegExp('\\{'+i+'\\}', 'gi');
        formatted = formatted.replace(regexp, arguments[i]);
    }
    return formatted;
};

function RyyppyAPI() {
    this.getUserData = function(userId, callback) {
        $.get('/API/users/{0}/'.format(userId), callback);
    }

    this.getUserDrinks = function(userId, callback) {
        $.get("/API/users/{0}/show-drinks".format(userId), callback);
    }
    
    this.getUserHistory = function(userId, callback) {
        $.get('/API/users/{0}/show-history'.format(userId), callback);
    }
    
    this.addDrinkToUser = function(userId, volume, alcohol, successCallback, errorCallback) {
        $.ajax(
            {
                url: '/API/users/{0}/add-drink'.format(userId),
                data: {
                    'volume': volume,
                    'alcohol': alcohol
                }
            })
            .success(successCallback)
            .error(errorCallback);
    }

    this.removeDrinkFromUser = function(userId, drinkId, callback) {
        $.get('/API/users/{0}/remove-drink/{1}'.format(userId, drinkId), callback);
    }

    this.getPartyData = function(partyId, callback) {
        $.get('/API/parties/{0}/'.format(partyId), callback);
    }
    
    this.addAnonymousUserToParty = function(partyId, name, sex, weight, successCallback, errorCallback) {
        $.ajax(
            {
                url: '/API/parties/{0}/add-anonymous-user'.format(partyId),
                data: {
                    'name': name,
                    'sex': sex,
                    'weight': weight
                }
            })
            .success(successCallback)
            .error(errorCallback);
    }
    
    this.linkUserToParty = function(partyId, userId, successCallback, errorCallback) {
        $.ajax(
            {
                url: '/API/parties/{0}/link-user-to-party/{1}'.format(partyId, userId)
            })
            .success(successCallback)
            .error(errorCallback);        
    }
}
