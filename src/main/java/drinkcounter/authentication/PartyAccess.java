package drinkcounter.authentication;

import drinkcounter.DrinkCounterService;
import org.springframework.stereotype.Component;

/**
 * Party-membership rules for the access annotations ({@link PartyMember}, {@link OwnUserOrPartyMate}).
 * Takes the signed-in user's id from the principal, so a check costs one query and no user load.
 */
@Component("partyAccess")
public class PartyAccess {

    private final DrinkCounterService drinkCounterService;

    public PartyAccess(DrinkCounterService drinkCounterService) {
        this.drinkCounterService = drinkCounterService;
    }

    public boolean isMember(int partyId, int userId) {
        return drinkCounterService.isUserParticipant(partyId, userId);
    }

    public boolean isPartyMate(int userId, int otherUserId) {
        return drinkCounterService.shareParty(userId, otherUserId);
    }
}
