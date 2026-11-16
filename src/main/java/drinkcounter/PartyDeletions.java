package drinkcounter;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.PartyJoinLinkRepository;
import drinkcounter.repository.PartyLocationInviteRepository;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Deleting a party. Its guests who are in no other party go with it, drinks included; users'
 * drinks stay in their own logs, since a drink belongs to its drinker, not to a party.
 */
@Service
public class PartyDeletions {

    private static final Logger log = LoggerFactory.getLogger(PartyDeletions.class);

    private final PartyRepository parties;
    private final PartyJoinLinkRepository joinLinks;
    private final PartyLocationInviteRepository locationInvites;
    private final DrinkRepository drinks;
    private final UserRepository users;

    public PartyDeletions(PartyRepository parties, PartyJoinLinkRepository joinLinks,
            PartyLocationInviteRepository locationInvites, DrinkRepository drinks, UserRepository users) {
        this.parties = parties;
        this.joinLinks = joinLinks;
        this.locationInvites = locationInvites;
        this.drinks = drinks;
        this.users = users;
    }

    /**
     * What deleting the party would mean for the user asking: whether they may, the other
     * members with an account who lose the party, and the guests deleted with it.
     */
    public record Preview(Party party, boolean allowed, List<User> otherUsers, List<User> guests) {
    }

    @Transactional(readOnly = true)
    public Preview preview(int partyId, int userId) {
        Party party = parties.findById(partyId).orElseThrow(EntityNotFoundException::new);
        List<User> otherUsers = party.getParticipants().stream()
                .filter(member -> !member.isGuest() && member.getId() != userId)
                .toList();
        return new Preview(party, allowed(party, userId), otherUsers, parties.findGuestsOnlyIn(partyId));
    }

    /** As {@code PartyAccess.canDelete}, for a party whose participants are loaded. */
    public static boolean allowed(Party party, int userId) {
        boolean member = false;
        boolean otherUsers = false;
        for (User participant : party.getParticipants()) {
            if (participant.getId() == userId) {
                member = true;
            } else if (!participant.isGuest()) {
                otherUsers = true;
            }
        }
        return member && (Integer.valueOf(userId).equals(party.getStartedBy()) || !otherUsers);
    }

    /** Deletes the party for everyone; who may do so is checked by the caller. */
    @Transactional
    public void delete(int partyId) {
        Party party = parties.findById(partyId).orElseThrow(EntityNotFoundException::new);
        List<User> guests = parties.findGuestsOnlyIn(partyId);
        if (!guests.isEmpty()) {
            drinks.deleteByDrinkerIdIn(guests.stream().map(User::getId).toList());
        }
        joinLinks.deleteById(partyId);
        locationInvites.deleteById(partyId);
        parties.delete(party);
        users.deleteAll(guests);
        log.info("Party {}, id {} was deleted with {} guests", party.getName(), partyId, guests.size());
    }
}
