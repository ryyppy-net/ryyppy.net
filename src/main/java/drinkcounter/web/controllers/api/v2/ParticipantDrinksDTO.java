package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.User;

/**
 * The answer to adding, changing or removing a party participant's drink: the drink (null once
 * removed) and the participant as the party page shows them.
 */
public record ParticipantDrinksDTO(DrinkDTO drink, ParticipantDTO participant) {

    public static ParticipantDrinksDTO of(Drink drink, User participant, BloodAlcohol bloodAlcohol) {
        ParticipantDTO participantDTO = ParticipantDTO.fromUser(participant, bloodAlcohol);
        participantDTO.setHistory(PromilleHistory.of(bloodAlcohol));
        return new ParticipantDrinksDTO(drink == null ? null : DrinkDTO.fromDrink(drink), participantDTO);
    }
}
