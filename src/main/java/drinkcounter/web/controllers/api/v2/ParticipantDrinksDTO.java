package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.Drink;
import drinkcounter.model.User;

/**
 * The answer to adding, changing or removing a party participant's drink: the drink (null once
 * removed) and the participant as the party page shows them.
 */
public record ParticipantDrinksDTO(DrinkDTO drink, ParticipantDTO participant) {

    public static ParticipantDrinksDTO of(Drink drink, User participant) {
        ParticipantDTO participantDTO = ParticipantDTO.fromUser(participant);
        participantDTO.setHistory(PromilleHistory.forUser(participant));
        return new ParticipantDrinksDTO(drink == null ? null : DrinkDTO.fromDrink(drink), participantDTO);
    }
}
