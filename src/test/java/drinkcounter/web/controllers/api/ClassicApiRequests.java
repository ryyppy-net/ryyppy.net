package drinkcounter.web.controllers.api;

import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Requests to the classic /API endpoints, sent the way the jQuery UI sends them.
 */
final class ClassicApiRequests {

    private ClassicApiRequests() {
    }

    static MockHttpServletRequestBuilder partyXml(int partyId) {
        return get("/API/parties/{partyId}", partyId);
    }

    static MockHttpServletRequestBuilder addGuest(int partyId) {
        return get("/API/parties/{partyId}/add-anonymous-user", partyId)
                .param("name", "Vieras").param("sex", "MALE").param("weight", "80");
    }

    static MockHttpServletRequestBuilder linkUser(int partyId, int userId) {
        return get("/API/parties/{partyId}/link-user-to-party/{userId}", partyId, userId);
    }

    static MockHttpServletRequestBuilder showDrinks(int userId) {
        return get("/API/users/{userId}/show-drinks", userId);
    }

    static MockHttpServletRequestBuilder drinksPerDay(int userId) {
        return get("/API/users/{userId}/drinks", userId);
    }

    static MockHttpServletRequestBuilder userXml(int userId) {
        return get("/API/users/{userId}", userId);
    }

    static MockHttpServletRequestBuilder addDrink(int userId) {
        return get("/API/users/{userId}/add-drink", userId);
    }

    static MockHttpServletRequestBuilder editDrink(int userId, int drinkId) {
        return get("/API/users/{userId}/edit-drink/{drinkId}", userId, drinkId)
                .param("volume", "0.5").param("alcohol", "0.05");
    }

    static MockHttpServletRequestBuilder removeDrink(int userId, int drinkId) {
        return get("/API/users/{userId}/remove-drink/{drinkId}", userId, drinkId);
    }

    static MockHttpServletRequestBuilder showHistory(int userId) {
        return get("/API/users/{userId}/show-history", userId);
    }

    static MockHttpServletRequestBuilder passphraseInfo(String passphrase) {
        return get("/API/passphrase/{passphrase}", passphrase);
    }

    static MockHttpServletRequestBuilder passphraseAddDrink(String passphrase) {
        return get("/API/passphrase/{passphrase}/add-drink/{time}", passphrase, 0);
    }

    static MockHttpServletRequestBuilder passphraseUndoDrink(String passphrase) {
        return get("/API/passphrase/{passphrase}/undo-drink", passphrase);
    }
}
