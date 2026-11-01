package drinkcounter.web.controllers.api.v2;

import drinkcounter.NearbyPartyPreview;
import drinkcounter.RoughDistance;
import drinkcounter.model.Party;
import java.util.Date;

/** What a nearby party reveals before joining: no participants, only its name, times and rough distance. */
public record NearbyPartyPreviewDTO(Integer partyId, String name, Date startTime, Date endTime,
        RoughDistance distance, boolean alreadyMember) {

    public static NearbyPartyPreviewDTO of(NearbyPartyPreview preview) {
        Party party = preview.nearby().party();
        return new NearbyPartyPreviewDTO(party.getId(), party.getName(), Date.from(party.getStartTime()),
                Date.from(party.getEndTime()), preview.nearby().distance(), preview.alreadyMember());
    }
}
