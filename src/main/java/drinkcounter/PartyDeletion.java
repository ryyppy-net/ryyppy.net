package drinkcounter;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deleting a party for everyone. Users' drinks belong to them, not to the party, so they stay;
 * a guest exists only for its parties, so one in no other party goes with its drinks.
 */
@Service
public class PartyDeletion {

    private static final Logger log = LoggerFactory.getLogger(PartyDeletion.class);

    /** What deleting the party would take away, as seen by the user asking. */
    public record Impact(Party party, boolean deletable, List<User> otherUsers, List<User> deletedGuests) {
    }

    private final PartyRoster partyRoster;
    private final PartyRepository parties;
    private final UserRepository users;
    private final DrinkRepository drinks;
    private final PartyJoinLinks joinLinks;
    private final PartyLocationInvites locationInvites;

    public PartyDeletion(PartyRoster partyRoster, PartyRepository parties, UserRepository users,
            DrinkRepository drinks, PartyJoinLinks joinLinks, PartyLocationInvites locationInvites) {
        this.partyRoster = partyRoster;
        this.parties = parties;
        this.users = users;
        this.drinks = drinks;
        this.joinLinks = joinLinks;
        this.locationInvites = locationInvites;
    }

    @Transactional(readOnly = true)
    public Impact impact(int partyId, int userId) {
        Party party = partyRoster.get(partyId);
        List<User> otherUsers = party.getParticipants().stream()
                .filter(participant -> !participant.isGuest() && participant.getId() != userId)
                .toList();
        return new Impact(party, party.canBeDeletedBy(userId), otherUsers, guestsOnlyIn(party));
    }

    /** Deletes the party; the caller has checked that the user may. */
    @Transactional
    public Impact delete(int partyId, int userId) {
        Impact impact = impact(partyId, userId);
        Party party = impact.party();
        joinLinks.revoke(partyId);
        locationInvites.revoke(partyId);
        parties.delete(party);
        for (User guest : impact.deletedGuests()) {
            drinks.deleteAll(drinks.findByDrinker(guest));
            users.delete(guest);
        }
        log.info("Party {}, id {} was deleted by user {} with {} guests", party.getName(), partyId, userId,
                impact.deletedGuests().size());
        return impact;
    }

    private static List<User> guestsOnlyIn(Party party) {
        return party.getParticipants().stream()
                .filter(participant -> participant.isGuest() && participant.getParties().size() == 1)
                .toList();
    }
}
