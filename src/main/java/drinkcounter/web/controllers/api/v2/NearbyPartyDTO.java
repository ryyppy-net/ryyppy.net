package drinkcounter.web.controllers.api.v2;

import drinkcounter.NearbyParty;
import drinkcounter.RoughDistance;
import drinkcounter.model.Party;
import java.util.Date;

/** A party found nearby: no participants, only its name, times and rough distance. */
public record NearbyPartyDTO(Integer partyId, String name, Date startTime, Date endTime, RoughDistance distance) {

    public static NearbyPartyDTO of(NearbyParty nearby) {
        Party party = nearby.party();
        return new NearbyPartyDTO(party.getId(), party.getName(), Date.from(party.getStartTime()),
                Date.from(party.getEndTime()), nearby.distance());
    }
}
