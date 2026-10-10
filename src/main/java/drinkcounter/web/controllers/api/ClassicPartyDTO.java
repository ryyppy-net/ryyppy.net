package drinkcounter.web.controllers.api;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.time.Clock;
import java.util.List;
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

    public static ClassicPartyDTO fromParty(Party party, List<User> participants, Clock clock) {
        return new ClassicPartyDTO(party.getId(), party.getName(),
                participants.stream().map(user -> ClassicUserDTO.fromUser(user, clock)).toList());
    }
}
