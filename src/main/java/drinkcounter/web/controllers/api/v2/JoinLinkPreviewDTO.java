package drinkcounter.web.controllers.api.v2;

import drinkcounter.JoinLinkPreview;
import drinkcounter.model.Party;
import java.util.Date;

/** What a join link reveals before joining: no participants, only the party's name and times. */
public record JoinLinkPreviewDTO(Integer partyId, String name, Date startTime, Date endTime, boolean alreadyMember) {

    public static JoinLinkPreviewDTO of(JoinLinkPreview preview) {
        Party party = preview.party();
        return new JoinLinkPreviewDTO(party.getId(), party.getName(), Date.from(party.getStartTime()),
                Date.from(party.getEndTime()), preview.alreadyMember());
    }
}
