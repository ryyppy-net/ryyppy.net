package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;

/**
 * The answer to adding, changing or removing one of the signed-in user's own drinks: the drink
 * (null once removed) and the profile, whose promilles the drink changes.
 */
public record OwnDrinksDTO(DrinkDTO drink, UserDTO profile) {

    public static OwnDrinksDTO of(Drink drink, BloodAlcohol bloodAlcohol) {
        return new OwnDrinksDTO(drink == null ? null : DrinkDTO.fromDrink(drink), OwnDrinksView.profile(bloodAlcohol));
    }
}
