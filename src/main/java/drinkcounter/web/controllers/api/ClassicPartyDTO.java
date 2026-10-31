package drinkcounter.web.controllers.api;

import drinkcounter.alcoholcalculator.BloodAlcohol;
import drinkcounter.model.Party;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * The party XML the classic UI reads from /API/parties/{id}.
 */
@JacksonXmlRootElement(localName = "party")
public record ClassicPartyDTO(
        int id,
        String name,
        @JacksonXmlElementWrapper(localName = "users") @JacksonXmlProperty(localName = "user")
        List<ClassicUserDTO> users) {

    /** @param lastDrinkTimes when each participant last drank, by user id */
    public static ClassicPartyDTO fromParty(Party party, List<BloodAlcohol> participants,
            Map<Integer, Instant> lastDrinkTimes, Clock clock) {
        return new ClassicPartyDTO(party.getId(), party.getName(),
                participants.stream()
                        .map(participant -> ClassicUserDTO.fromUser(participant, lastDrinkTimes.get(participant.user().getId()), clock))
                        .toList());
    }
}
