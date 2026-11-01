package drinkcounter;

import drinkcounter.authentication.PartyAccess;
import drinkcounter.model.Party;
import drinkcounter.model.PartyLocationInvite;
import drinkcounter.repository.InvitedParty;
import drinkcounter.repository.PartyLocationInviteRepository;
import drinkcounter.repository.PartyRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Finding and joining parties by location: a signed-in user within a party's location invite
 * radius can see it and join it until it ends. The user's position is only compared, never kept.
 */
@Service
public class NearbyParties {

    private static final double METRES_PER_DEGREE_LATITUDE = Position.EARTH_RADIUS_M * Math.PI / 180;

    private final PartyLocationInviteRepository invites;
    private final PartyRepository parties;
    private final PartyRoster partyRoster;
    private final PartyAccess partyAccess;

    public NearbyParties(PartyLocationInviteRepository invites, PartyRepository parties, PartyRoster partyRoster,
            PartyAccess partyAccess) {
        this.invites = invites;
        this.parties = parties;
        this.partyRoster = partyRoster;
        this.partyAccess = partyAccess;
    }

    /** Parties whose invite radius contains the position, nearest first. */
    @Transactional(readOnly = true)
    public List<NearbyParty> around(Position position) {
        Instant now = Instant.now();
        double latDelta = PartyLocationInvites.MAX_RADIUS_M / METRES_PER_DEGREE_LATITUDE;
        double cosLat = Math.cos(Math.toRadians(position.latitude()));
        double lonDelta = cosLat > 0 ? latDelta / cosLat : Double.POSITIVE_INFINITY;
        double minLon = position.longitude() - lonDelta;
        double maxLon = position.longitude() + lonDelta;
        // Near a pole or across the antimeridian the box would wrap; fall back to every longitude.
        if (minLon < -180 || maxLon > 180) {
            minLon = -180;
            maxLon = 180;
        }
        return invites.findInBoxOfActiveParties(position.latitude() - latDelta, position.latitude() + latDelta,
                        minLon, maxLon, now).stream()
                .filter(found -> !found.party().hasEnded(now))
                .map(found -> new Reach(found.party(), position.distanceTo(placeOf(found.invite())),
                        found.invite().getRadiusM()))
                .filter(Reach::within)
                .sorted(Comparator.comparingDouble(Reach::metres).thenComparing(reach -> reach.party().getId()))
                .map(Reach::toNearbyParty)
                .toList();
    }

    @Transactional(readOnly = true)
    public NearbyPartyPreview preview(int partyId, Position position, int userId) {
        NearbyParty nearby = usable(partyId, position);
        return new NearbyPartyPreview(nearby, partyAccess.isMember(partyId, userId));
    }

    /** Adds the user to the party; a user who is already a member stays one. */
    @Transactional
    public Party join(int partyId, Position position, int userId) {
        Party party = usable(partyId, position).party();
        partyRoster.join(party.getId(), userId);
        return party;
    }

    // A party's end time can move, so it is checked against now at each use.
    private NearbyParty usable(int partyId, Position position) {
        Optional<PartyLocationInvite> invite = invites.findById(partyId);
        Optional<Party> party = invite.flatMap(found -> parties.findById(partyId));
        if (invite.isEmpty() || party.isEmpty() || party.get().hasEnded(Instant.now())) {
            throw new UnusableLocationInviteException();
        }
        Reach reach = new Reach(party.get(), position.distanceTo(placeOf(invite.get())), invite.get().getRadiusM());
        if (!reach.within()) {
            throw new UnusableLocationInviteException();
        }
        return reach.toNearbyParty();
    }

    private static Position placeOf(PartyLocationInvite invite) {
        return new Position(invite.getLatitude(), invite.getLongitude());
    }

    private record Reach(Party party, double metres, int radiusM) {
        boolean within() {
            return metres <= radiusM;
        }

        NearbyParty toNearbyParty() {
            return new NearbyParty(party, RoughDistance.of(metres));
        }
    }
}
