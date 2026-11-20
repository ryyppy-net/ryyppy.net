package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartyDeletionTest {

    private final PartyRoster partyRoster = mock(PartyRoster.class);
    private final PartyRepository parties = mock(PartyRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final DrinkRepository drinks = mock(DrinkRepository.class);
    private final PartyJoinLinks joinLinks = mock(PartyJoinLinks.class);
    private final PartyLocationInvites locationInvites = mock(PartyLocationInvites.class);
    private final PartyDeletion deletion = new PartyDeletion(partyRoster, parties, users, drinks, joinLinks, locationInvites);

    private final Party party = new Party();
    private final Party otherParty = new Party();
    private final User matti = user(7, false);
    private final User liisa = user(8, false);
    private final User onlyHere = user(9, true);
    private final User alsoElsewhere = user(10, true);

    PartyDeletionTest() {
        party.setId(5);
        party.setStartedBy(7);
        for (User participant : List.of(matti, liisa, onlyHere, alsoElsewhere)) {
            party.addParticipant(participant);
            participant.getParties().add(party);
        }
        alsoElsewhere.getParties().add(otherParty);
        when(partyRoster.get(5)).thenReturn(party);
    }

    @Test
    void theImpactNamesTheOtherUsersAndTheGuestsInNoOtherParty() {
        PartyDeletion.Impact impact = deletion.impact(5, 7);

        assertEquals(true, impact.deletable());
        assertEquals(List.of(liisa), impact.otherUsers());
        assertEquals(List.of(onlyHere), impact.deletedGuests());
    }

    @Test
    void deletingRemovesThePartyItsInvitesAndItsOwnGuestsButNoUserOrTheirDrinks() {
        List<Drink> guestDrinks = List.of(new Drink());
        when(drinks.findByDrinker(onlyHere)).thenReturn(guestDrinks);

        deletion.delete(5, 7);

        verify(joinLinks).revoke(5);
        verify(locationInvites).revoke(5);
        verify(parties).delete(party);
        verify(drinks).deleteAll(guestDrinks);
        verify(users).delete(onlyHere);
        verify(users, never()).delete(alsoElsewhere);
        verify(users, never()).delete(matti);
        verify(users, never()).delete(liisa);
        verify(drinks, never()).findByDrinker(matti);
        verify(drinks, never()).findByDrinker(liisa);
    }

    private static User user(int id, boolean guest) {
        User user = new User();
        user.setId(id);
        user.setGuest(guest);
        user.setParties(new ArrayList<>());
        return user;
    }
}
