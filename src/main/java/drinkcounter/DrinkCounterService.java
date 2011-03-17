/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter;

import drinkcounter.model.Drink;
import drinkcounter.model.Participant;
import drinkcounter.model.Party;
import java.util.List;

/**
 *
 * @author Toni
 */
public interface DrinkCounterService {

    // Party methods
    Party startParty(String identifier);
    void updateParty(Party party);
    List<Party> listParties();
    Party getParty(String identifier);
    String getPartyIdForParticipant(String participantId);

    // Participants
    List<Participant> listParticipants(String partyIdentifier);
    Participant addParticipant(Participant participant, String partyIdentifier);
    Participant getParticipant(String participantid);
    void deleteParticipant(String participantId);
    void addDrink(String participantIdentifier);

    // Global
    void timePassed(float hours);
    List<Drink> getDrinks(String participantIdentifier);
    void archiveParty(String partyId);
}
