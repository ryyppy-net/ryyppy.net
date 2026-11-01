package drinkcounter;

import drinkcounter.model.PartyLocationInvite;
import drinkcounter.repository.PartyLocationInviteRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Each party's one location invite, which any member can turn on, change or turn off: a place
 * and a radius around it.
 */
@Service
public class PartyLocationInvites {

    public static final int MIN_RADIUS_M = 100;
    public static final int MAX_RADIUS_M = 5000;

    private final PartyLocationInviteRepository invites;

    public PartyLocationInvites(PartyLocationInviteRepository invites) {
        this.invites = invites;
    }

    public Optional<PartyLocationInvite> current(int partyId) {
        return invites.findById(partyId);
    }

    /** Turns the party's invite on at this place, or moves the existing one there. */
    @Transactional
    public PartyLocationInvite set(int partyId, double latitude, double longitude, int radiusM, int byUserId) {
        requireValid(latitude, longitude, radiusM);
        PartyLocationInvite invite = invites.findById(partyId).orElseGet(PartyLocationInvite::new);
        invite.setPartyId(partyId);
        invite.setLatitude(latitude);
        invite.setLongitude(longitude);
        invite.setRadiusM(radiusM);
        invite.setCreatedBy(byUserId);
        invite.setCreatedAt(Instant.now());
        return invites.save(invite);
    }

    @Transactional
    public void revoke(int partyId) {
        invites.deleteById(partyId);
    }

    public static void requireValid(double latitude, double longitude, int radiusM) {
        if (!(latitude >= -90 && latitude <= 90)) {
            throw new InvalidLocationInviteException("Latitude must be between -90 and 90");
        }
        if (!(longitude >= -180 && longitude <= 180)) {
            throw new InvalidLocationInviteException("Longitude must be between -180 and 180");
        }
        if (radiusM < MIN_RADIUS_M || radiusM > MAX_RADIUS_M) {
            throw new InvalidLocationInviteException(
                    "Radius must be between " + MIN_RADIUS_M + " and " + MAX_RADIUS_M + " m");
        }
    }
}
