package drinkcounter.web.controllers.api.v2;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;

/**
 * The answer to adding, changing or removing a party participant's drink: the drink (null once
 * removed) and the participant as the party page shows them.
 */
public record ParticipantDrinksDTO(DrinkDTO drink, ParticipantDTO participant) {

    /** @param bloodAlcohol the participant's, from {@link drinkcounter.Promilles#during} the party */
    public static ParticipantDrinksDTO of(Drink drink, BloodAlcohol bloodAlcohol, Party party) {
        ParticipantDTO participantDTO = ParticipantDTO.inParty(bloodAlcohol);
        participantDTO.setHistory(PromilleHistory.during(bloodAlcohol, party.getStartTime()));
        return new ParticipantDrinksDTO(drink == null ? null : DrinkDTO.fromDrink(drink), participantDTO);
    }
}
