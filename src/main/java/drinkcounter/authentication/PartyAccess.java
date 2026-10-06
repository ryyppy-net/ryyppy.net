package drinkcounter.authentication;

import drinkcounter.DrinkCounterService;
import org.springframework.stereotype.Component;

/**
 * Party-membership rules for the access annotations ({@link PartyMember}, {@link OwnUserOrPartyMate}).
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
