package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import java.time.Clock;
import java.util.List;

/**
 * The answer to adding, changing or removing one of the signed-in user's own drinks: the drink
 * (null once removed) and everything the dashboard shows that the drink changes.
 */
public record OwnDrinksDTO(DrinkDTO drink, UserDTO profile, List<DrinkDTO> latestDrinks, String drinkHistory) {

    public static OwnDrinksDTO of(Drink drink, BloodAlcohol bloodAlcohol, Clock clock) {
        return new OwnDrinksDTO(drink == null ? null : DrinkDTO.fromDrink(drink),
                OwnDrinksView.profile(bloodAlcohol),
                OwnDrinksView.latestDrinks(bloodAlcohol.user()),
                OwnDrinksView.drinkHistory(bloodAlcohol.user(), clock));
    }
}
