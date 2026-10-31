package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;

/**
 * What the dashboard shows of the signed-in user: DefaultController embeds it on load, so the
 * AngularJS stores need not fetch it again.
 */
public final class OwnDrinksView {

    private OwnDrinksView() {
    }

    public static UserDTO profile(BloodAlcohol bloodAlcohol) {
        UserDTO userDTO = UserDTO.fromUser(bloodAlcohol);
        userDTO.setHistory(PromilleHistory.of(bloodAlcohol));
        return userDTO;
    }
}
