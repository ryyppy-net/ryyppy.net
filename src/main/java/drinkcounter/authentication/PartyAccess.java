package drinkcounter.authentication;

import drinkcounter.repository.PartyRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Party-membership rules for the access annotations ({@link PartyMember}, {@link OwnUserOrPartyMate},
 * {@link PartyStarterOrOnlyUser}).
 */
@Component("partyAccess")
public class PartyAccess {

    private final PartyRepository partyRepository;

    public PartyAccess(PartyRepository partyRepository) {
        this.partyRepository = partyRepository;
    }

    public boolean isMember(int partyId, int userId) {
        return partyRepository.countUserParticipations(partyId, userId) > 0;
    }

    public boolean isPartyMate(int userId, int otherUserId) {
        return partyRepository.countSharedParties(userId, otherUserId) > 0;
    }

    @Transactional(readOnly = true)
    public boolean canDelete(int partyId, int userId) {
        return partyRepository.findById(partyId).map(party -> party.canBeDeletedBy(userId)).orElse(false);
    }
}
