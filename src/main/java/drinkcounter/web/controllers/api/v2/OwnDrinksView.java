package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.User;
import java.util.List;

/**
 * What the dashboard shows of the signed-in user: DefaultController embeds it on load, so the
 * AngularJS stores need not fetch it again.
 */
public final class OwnDrinksView {

    private OwnDrinksView() {}

    public static UserDTO profile(BloodAlcohol bloodAlcohol) {
        UserDTO userDTO = UserDTO.fromUser(bloodAlcohol);
        userDTO.setHistory(PromilleHistory.of(bloodAlcohol));
        return userDTO;
    }

    // Every drink, oldest first: the dashboard lists the newest and graphs them all per day.
    public static List<DrinkDTO> drinks(User user) {
        return user.getDrinks().stream().map(DrinkDTO::fromDrink).toList();
    }
}
