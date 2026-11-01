package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import drinkcounter.model.PartyLocationInvite;
import drinkcounter.repository.PartyLocationInviteRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PartyLocationInvitesTest {

    private final PartyLocationInviteRepository repository = mock(PartyLocationInviteRepository.class);
    private final PartyLocationInvites invites = new PartyLocationInvites(repository);

    @BeforeEach
    void saveReturnsTheSavedInvite() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newInviteRecordsThePlaceRadiusCreatorAndWhenItWasSet() {
        Instant before = Instant.now();

        PartyLocationInvite invite = invites.set(1, 60.1699, 24.9384, 1000, 42);

        assertEquals(1, invite.getPartyId());
        assertEquals(60.1699, invite.getLatitude());
        assertEquals(24.9384, invite.getLongitude());
        assertEquals(1000, invite.getRadiusM());
        assertEquals(42, invite.getCreatedBy());
        assertFalse(invite.getCreatedAt().isBefore(before));
        assertFalse(invite.getCreatedAt().isAfter(Instant.now()));
        verify(repository).save(invite);
    }

    @Test
    void settingAgainReplacesThePartysInvite() {
        PartyLocationInvite existing = new PartyLocationInvite();
        existing.setPartyId(1);
        existing.setLatitude(61.4978);
        existing.setLongitude(23.7610);
        existing.setRadiusM(500);
        existing.setCreatedBy(7);
        existing.setCreatedAt(Instant.parse("2024-03-05T12:00:00Z"));
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        PartyLocationInvite invite = invites.set(1, 60.1699, 24.9384, 2000, 42);

        assertSame(existing, invite);
        assertEquals(60.1699, invite.getLatitude());
        assertEquals(24.9384, invite.getLongitude());
        assertEquals(2000, invite.getRadiusM());
        assertEquals(42, invite.getCreatedBy());
        assertNotEquals(Instant.parse("2024-03-05T12:00:00Z"), invite.getCreatedAt());
    }

    @Test
    void currentIsThePartysStoredInvite() {
        PartyLocationInvite existing = new PartyLocationInvite();
        when(repository.findById(1)).thenReturn(Optional.of(existing));

        assertSame(existing, invites.current(1).orElseThrow());
        assertTrue(invites.current(2).isEmpty());
    }

    @Test
    void revokingDeletesThePartysInvite() {
        invites.revoke(1);

        verify(repository).deleteById(1);
    }

    @ParameterizedTest
    @CsvSource({
            "-90, -180, 100",
            "90, 180, 5000",
            "0, 0, 1000"
    })
    void placesOnEarthWithARadiusFromOneHundredMetresToFiveKilometresAreAccepted(
            double latitude, double longitude, int radiusM) {
        invites.set(1, latitude, longitude, radiusM, 42);

        verify(repository).save(any());
    }

    @ParameterizedTest
    @CsvSource({
            "-90.0001, 0, 1000",
            "90.0001, 0, 1000",
            "NaN, 0, 1000",
            "0, -180.0001, 1000",
            "0, 180.0001, 1000",
            "0, NaN, 1000",
            "0, 0, 99",
            "0, 0, 5001",
            "0, 0, -1000"
    })
    void invalidPlaceOrRadiusIsRejectedAndNothingIsSaved(double latitude, double longitude, int radiusM) {
        assertThrows(InvalidLocationInviteException.class, () -> invites.set(1, latitude, longitude, radiusM, 42));

        verify(repository, never()).save(any());
    }
}
