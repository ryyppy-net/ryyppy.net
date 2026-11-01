package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.PartyLocationInvite;
import java.util.Date;

public record LocationInviteDTO(double latitude, double longitude, int radiusM, Date createdAt) {

    static LocationInviteDTO of(PartyLocationInvite invite) {
        return new LocationInviteDTO(invite.getLatitude(), invite.getLongitude(), invite.getRadiusM(),
                Date.from(invite.getCreatedAt()));
    }
}
