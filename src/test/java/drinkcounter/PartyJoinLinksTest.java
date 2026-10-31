package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import drinkcounter.model.PartyJoinLink;
import drinkcounter.repository.PartyJoinLinkRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PartyJoinLinksTest {

    private final PartyJoinLinkRepository repository = mock(PartyJoinLinkRepository.class);
    private final PartyJoinLinks joinLinks = new PartyJoinLinks(repository);

    @BeforeEach
    void saveReturnsTheSavedLink() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newLinkHasAnUnpaddedBase64UrlTokenOfSixteenBytes() {
        PartyJoinLink link = joinLinks.regenerate(1, 42);

        assertTrue(link.getToken().matches("[A-Za-z0-9_-]{22}"), link.getToken());
    }

    @Test
    void newLinkRecordsThePartyItsCreatorAndWhenItWasCreated() {
        Instant before = Instant.now();

        PartyJoinLink link = joinLinks.regenerate(1, 42);

        assertEquals(1, link.getPartyId());
        assertEquals(42, link.getCreatedBy());
        assertFalse(link.getCreatedAt().isBefore(before));
        assertFalse(link.getCreatedAt().isAfter(Instant.now()));
        verify(repository).save(link);
    }

    @Test
    void regeneratingReplacesTheTokenOfThePartysLink() {
        PartyJoinLink existing = new PartyJoinLink();
        existing.setPartyId(1);
        existing.setToken("old-token");
        existing.setCreatedBy(7);
        existing.setCreatedAt(Instant.parse("2024-03-05T12:00:00Z"));
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        PartyJoinLink link = joinLinks.regenerate(1, 42);

        assertSame(existing, link);
        assertNotEquals("old-token", link.getToken());
        assertEquals(42, link.getCreatedBy());
        assertNotEquals(Instant.parse("2024-03-05T12:00:00Z"), link.getCreatedAt());
    }

    @Test
    void eachLinkGetsADifferentToken() {
        assertNotEquals(joinLinks.regenerate(1, 42).getToken(), joinLinks.regenerate(2, 42).getToken());
    }

    @Test
    void currentIsThePartysStoredLink() {
        PartyJoinLink existing = new PartyJoinLink();
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        assertSame(existing, joinLinks.current(1).orElseThrow());
        assertTrue(joinLinks.current(2).isEmpty());
    }

    @Test
    void revokingDeletesThePartysLink() {
        joinLinks.revoke(1);

        verify(repository).deleteById(1);
    }
}
