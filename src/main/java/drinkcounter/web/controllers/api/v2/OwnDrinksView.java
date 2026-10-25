package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.User;
import drinkcounter.util.DrinksPerDayCsv;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.List;

/**
 * What the dashboard shows of the signed-in user: DefaultController embeds it on load and
 * the own-drink writes return it, so the AngularJS stores need not fetch it again.
 */
public final class OwnDrinksView {

    // The user page lists this many of the newest drinks; RyyppyAPI.getOwnDrinks asks for the same.
    public static final int LATEST_DRINKS = 5;

    private OwnDrinksView() {
    }

    public static UserDTO profile(User user, BloodAlcohol bloodAlcohol) {
        UserDTO userDTO = UserDTO.fromUser(user, bloodAlcohol);
        userDTO.setHistory(PromilleHistory.of(bloodAlcohol));
        return userDTO;
    }

    public static List<DrinkDTO> latestDrinks(User user) {
        return user.latestDrinks(LATEST_DRINKS).stream().map(DrinkDTO::fromDrink).toList();
    }

    public static String drinkHistory(User user, Clock clock) {
        try {
            return DrinksPerDayCsv.build(user.getDrinks(), clock);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
