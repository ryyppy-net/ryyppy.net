package drinkcounter.authentication;

import drinkcounter.dao.PartyDAO;
import org.springframework.stereotype.Component;

/**
 * Party-membership rules for the access annotations ({@link PartyMember}, {@link OwnUserOrPartyMate}).
 */
@Component("partyAccess")
public class PartyAccess {

    private final PartyDAO partyDao;

    public PartyAccess(PartyDAO partyDao) {
        this.partyDao = partyDao;
    }

    public boolean isMember(int partyId, int userId) {
        return partyDao.countUserParticipations(partyId, userId) > 0;
    }

    public boolean isPartyMate(int userId, int otherUserId) {
        return partyDao.countSharedParties(userId, otherUserId) > 0;
    }
}
