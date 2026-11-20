package drinkcounter.model;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PartyTest {

    @Test
    void theStarterCanDeleteThePartyWhileTheyAreAMember() {
        Party party = partyWith(user(1), user(2));
        party.setStartedBy(1);

        assertTrue(party.canBeDeletedBy(1));
        assertFalse(party.canBeDeletedBy(2));
    }

    @Test
    void theOnlyUserLeftCanDeleteThePartyWhoeverStartedIt() {
        Party party = partyWith(user(2), guest(3));
        party.setStartedBy(1);

        assertTrue(party.canBeDeletedBy(2));
    }

    @Test
    void theOnlyUserLeftCanDeleteAPartyWithNoRecordedStarter() {
        assertTrue(partyWith(user(2)).canBeDeletedBy(2));
        assertFalse(partyWith(user(1), user(2)).canBeDeletedBy(2));
    }

    @Test
    void aStarterWhoLeftCannotDeleteTheParty() {
        Party party = partyWith(user(2), user(3));
        party.setStartedBy(1);

        assertFalse(party.canBeDeletedBy(1));
    }

    private static Party partyWith(User... participants) {
        Party party = new Party();
        for (User participant : participants) {
            party.addParticipant(participant);
        }
        return party;
    }

    private static User user(int id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static User guest(int id) {
        User guest = user(id);
        guest.setGuest(true);
        return guest;
    }
}
