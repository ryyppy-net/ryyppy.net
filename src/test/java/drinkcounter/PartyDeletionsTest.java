package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.repository.PartyJoinLinkRepository;
import drinkcounter.repository.PartyLocationInviteRepository;
import drinkcounter.repository.PartyRepository;
import drinkcounter.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class PartyDeletionsTest {

    private final PartyRepository parties = mock(PartyRepository.class);
    private final PartyJoinLinkRepository joinLinks = mock(PartyJoinLinkRepository.class);
    private final PartyLocationInviteRepository locationInvites = mock(PartyLocationInviteRepository.class);
    private final DrinkRepository drinks = mock(DrinkRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final PartyDeletions deletions = new PartyDeletions(parties, joinLinks, locationInvites, drinks, users);

    private final User matti = user(7, false);
    private final User liisa = user(8, false);
    private final User guest = user(9, true);

    @Test
    void starterMayDeleteAPartyOtherUsersAreIn() {
        Party party = party(7, matti, liisa);

        assertTrue(PartyDeletions.allowed(party, 7));
    }

    @Test
    void memberWhoDidNotStartThePartyMayNotDeleteItWhileOtherUsersAreIn() {
        Party party = party(7, matti, liisa);

        assertFalse(PartyDeletions.allowed(party, 8));
    }

    @Test
    void onlyUserLeftMayDeleteThePartyWhoeverStartedItAndWhateverGuestsAreIn() {
        assertTrue(PartyDeletions.allowed(party(8, matti, guest), 7));
        assertTrue(PartyDeletions.allowed(party(null, matti), 7));
    }

    @Test
    void starterWhoLeftThePartyMayNotDeleteIt() {
        assertFalse(PartyDeletions.allowed(party(7, liisa), 7));
    }

    @Test
    void previewListsTheOtherUsersAndTheGuestsDeletedWithTheParty() {
        Party party = party(7, matti, liisa, guest);
        when(parties.findById(1)).thenReturn(Optional.of(party));
        when(parties.findGuestsOnlyIn(1)).thenReturn(List.of(guest));

        PartyDeletions.Preview preview = deletions.preview(1, 7);

        assertTrue(preview.allowed());
        assertEquals(List.of(liisa), preview.otherUsers());
        assertEquals(List.of(guest), preview.guests());
    }

    @Test
    void deletingRemovesThePartyItsSharingAndItsOwnGuestsWithTheirDrinks() {
        Party party = party(7, matti, liisa, guest);
        when(parties.findById(1)).thenReturn(Optional.of(party));
        when(parties.findGuestsOnlyIn(1)).thenReturn(List.of(guest));

        deletions.delete(1);

        InOrder order = inOrder(drinks, joinLinks, locationInvites, parties, users);
        order.verify(drinks).deleteByDrinkerIdIn(List.of(9));
        order.verify(joinLinks).deleteById(1);
        order.verify(locationInvites).deleteById(1);
        order.verify(parties).delete(party);
        order.verify(users).deleteAll(List.of(guest));
    }

    @Test
    void deletingAPartyWithoutGuestsDeletesNoDrinks() {
        Party party = party(7, matti, liisa);
        when(parties.findById(1)).thenReturn(Optional.of(party));
        when(parties.findGuestsOnlyIn(1)).thenReturn(List.of());

        deletions.delete(1);

        verify(drinks, never()).deleteByDrinkerIdIn(any());
        verify(parties).delete(party);
    }

    private static Party party(Integer startedBy, User... members) {
        Party party = new Party();
        party.setId(1);
        party.setName("Sauna night");
        party.setStartedBy(startedBy);
        party.getParticipants().addAll(List.of(members));
        return party;
    }

    private static User user(int id, boolean guest) {
        User user = new User();
        user.setId(id);
        user.setName("User " + id);
        user.setGuest(guest);
        return user;
    }
}
