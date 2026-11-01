package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import drinkcounter.authentication.PartyAccess;
import drinkcounter.model.Party;
import drinkcounter.model.PartyLocationInvite;
import drinkcounter.repository.InvitedParty;
import drinkcounter.repository.PartyLocationInviteRepository;
import drinkcounter.repository.PartyRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class NearbyPartiesTest {

    private static final double METRES_PER_DEGREE_LATITUDE = 111_194.93;
    private static final Position PLACE = new Position(60.1699, 24.9384);

    private final PartyLocationInviteRepository invites = mock(PartyLocationInviteRepository.class);
    private final PartyRepository parties = mock(PartyRepository.class);
    private final PartyRoster partyRoster = mock(PartyRoster.class);
    private final PartyAccess partyAccess = mock(PartyAccess.class);
    private final NearbyParties nearbyParties = new NearbyParties(invites, parties, partyRoster, partyAccess);
    private final List<InvitedParty> inBox = new ArrayList<>();

    @Test
    void searchFindsPartiesWhoseRadiusContainsThePositionNearestFirst() {
        Party far = invitedParty(1, north(PLACE, 2400), Duration.ofHours(1), 5000);
        Party near = invitedParty(2, north(PLACE, 300), Duration.ofHours(1), 1000);
        invitedParty(3, north(PLACE, 150), Duration.ofHours(1), 100);
        stubSearch();

        List<NearbyParty> found = nearbyParties.around(PLACE);

        assertEquals(2, found.size());
        assertSame(near, found.get(0).party());
        assertEquals(new RoughDistance(300, false), found.get(0).distance());
        assertSame(far, found.get(1).party());
        assertEquals(new RoughDistance(2500, false), found.get(1).distance());
    }

    @Test
    void searchLeavesOutAPartyThatHasEnded() {
        invitedParty(1, Duration.ofSeconds(-1), 1000);
        stubSearch();

        assertTrue(nearbyParties.around(PLACE).isEmpty());
    }

    @Test
    void searchAsksOnlyForInvitesWithinTheLargestRadiusOfThePosition() {
        stubSearch();

        nearbyParties.around(PLACE);

        ArgumentCaptor<Double> minLat = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLat = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> minLon = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<Double> maxLon = ArgumentCaptor.forClass(Double.class);
        verify(invites).findInBoxOfActiveParties(minLat.capture(), maxLat.capture(), minLon.capture(),
                maxLon.capture(), any());
        double latDelta = PartyLocationInvites.MAX_RADIUS_M / METRES_PER_DEGREE_LATITUDE;
        assertEquals(PLACE.latitude() - latDelta, minLat.getValue(), 1e-6);
        assertEquals(PLACE.latitude() + latDelta, maxLat.getValue(), 1e-6);
        double lonDelta = latDelta / Math.cos(Math.toRadians(PLACE.latitude()));
        assertEquals(PLACE.longitude() - lonDelta, minLon.getValue(), 1e-6);
        assertEquals(PLACE.longitude() + lonDelta, maxLon.getValue(), 1e-6);
    }

    @Test
    void searchNextToTheAntimeridianAsksForEveryLongitude() {
        stubSearch();

        nearbyParties.around(new Position(-16.5, 179.99));

        verify(invites).findInBoxOfActiveParties(anyDouble(), anyDouble(), eq(-180.0), eq(180.0), any());
    }

    @Test
    void radiusIncludesItsEdge() {
        invitedParty(1, Duration.ofHours(1), 1000);

        assertEquals(1, nearbyParties.preview(1, north(PLACE, 999.9), 42).nearby().party().getId());
        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.preview(1, north(PLACE, 1000.1), 42));
    }

    @Test
    void previewShowsThePartyItsRoughDistanceAndWhetherTheUserIsAlreadyIn() {
        Party party = invitedParty(1, Duration.ofHours(1), 1000);
        when(partyAccess.isMember(1, 42)).thenReturn(true);

        NearbyPartyPreview preview = nearbyParties.preview(1, north(PLACE, 40), 42);

        assertSame(party, preview.nearby().party());
        assertEquals(new RoughDistance(100, true), preview.nearby().distance());
        assertTrue(preview.alreadyMember());
        assertFalse(nearbyParties.preview(1, PLACE, 43).alreadyMember());
    }

    @Test
    void joiningAddsOnlyTheUserWhoIsNearby() {
        Party party = invitedParty(1, Duration.ofHours(1), 1000);

        assertSame(party, nearbyParties.join(1, north(PLACE, 500), 42));

        verify(partyRoster).join(1, 42);
    }

    @Test
    void joiningFromOutsideTheRadiusAddsNobody() {
        invitedParty(1, Duration.ofHours(1), 1000);

        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.join(1, north(PLACE, 1500), 42));

        verify(partyRoster, never()).join(anyInt(), anyInt());
    }

    @Test
    void inviteStopsWorkingWhenThePartyEnds() {
        invitedParty(1, Duration.ofSeconds(-1), 1000);

        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.preview(1, PLACE, 42));
        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.join(1, PLACE, 42));
        verify(partyRoster, never()).join(anyInt(), anyInt());
    }

    @Test
    void partyWithoutAnInviteOrUnknownPartyIsUnusable() {
        Party withoutInvite = party(1, Duration.ofHours(1));
        when(parties.findById(1)).thenReturn(Optional.of(withoutInvite));

        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.preview(1, PLACE, 42));
        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.join(1, PLACE, 42));
        assertThrows(UnusableLocationInviteException.class, () -> nearbyParties.join(99, PLACE, 42));
        verify(partyRoster, never()).join(anyInt(), anyInt());
    }

    private void stubSearch() {
        when(invites.findInBoxOfActiveParties(anyDouble(), anyDouble(), anyDouble(), anyDouble(), any()))
                .thenReturn(inBox);
    }

    private Party invitedParty(int id, Duration untilEnd, int radiusM) {
        return invitedParty(id, PLACE, untilEnd, radiusM);
    }

    private Party invitedParty(int id, Position place, Duration untilEnd, int radiusM) {
        Party party = party(id, untilEnd);
        PartyLocationInvite invite = new PartyLocationInvite();
        invite.setPartyId(id);
        invite.setLatitude(place.latitude());
        invite.setLongitude(place.longitude());
        invite.setRadiusM(radiusM);
        when(invites.findById(id)).thenReturn(Optional.of(invite));
        when(parties.findById(id)).thenReturn(Optional.of(party));
        inBox.add(new InvitedParty(invite, party));
        return party;
    }

    private static Party party(int id, Duration untilEnd) {
        Party party = new Party();
        party.setId(id);
        party.setName("Party " + id);
        party.setStartTime(Instant.now().minus(Duration.ofHours(2)));
        party.setEndTime(Instant.now().plus(untilEnd));
        return party;
    }

    private static Position north(Position from, double metres) {
        return new Position(from.latitude() + metres / METRES_PER_DEGREE_LATITUDE, from.longitude());
    }
}
