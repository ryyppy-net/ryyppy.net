package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import drinkcounter.InvalidLocationInviteException;
import drinkcounter.InvitationSuggestions;
import drinkcounter.NearbyParties;
import drinkcounter.NearbyParty;
import drinkcounter.PartyJoinLinks;
import drinkcounter.PartyLocationInvites;
import drinkcounter.PartyRoster;
import drinkcounter.Position;
import drinkcounter.RoughDistance;
import drinkcounter.UnusableJoinLinkException;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.mcp.PartyTools.Candidate;
import drinkcounter.mcp.PartyTools.InviteReport;
import drinkcounter.mcp.PartyTools.JoinOutcome;
import drinkcounter.mcp.PartyTools.Person;
import drinkcounter.mcp.PartyTools.StartedParty;
import drinkcounter.mcp.PartyTools.PartyInvite;
import drinkcounter.mcp.PartyTools.PartySummary;
import drinkcounter.mcp.PartyTools.UpdatedParty;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.PartyJoinLink;
import drinkcounter.model.PartyLocationInvite;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PartyToolsTest {

    private final UserAccounts userAccounts = mock(UserAccounts.class);
    private final PartyRoster partyRoster = mock(PartyRoster.class);
    private final PartyAccess partyAccess = mock(PartyAccess.class);
    private final InvitationSuggestions invitationSuggestions = mock(InvitationSuggestions.class);
    private final PartyJoinLinks joinLinks = mock(PartyJoinLinks.class);
    private final PartyLocationInvites locationInvites = mock(PartyLocationInvites.class);
    private final NearbyParties nearbyParties = mock(NearbyParties.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final PartyTools tools = new PartyTools(userAccounts, partyRoster, partyAccess, invitationSuggestions,
            joinLinks, locationInvites, nearbyParties, request);

    @BeforeEach
    void signIn() {
        signInWithToken("matti@example.com");
        when(userAccounts.byEmail("matti@example.com")).thenReturn(user(7, "Matti Meikäläinen", "matti@example.com"));
        when(request.getScheme()).thenReturn("https");
        when(request.getServerName()).thenReturn("ryyppy.net");
        when(request.getServerPort()).thenReturn(443);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void listsThePartiesOfTheUserTheTokenWasIssuedTo() {
        Party ongoing = party(1, "Sauna night", Instant.now().minus(Duration.ofHours(2)), Instant.now().plus(Duration.ofHours(22)));
        Party ended = party(2, "Vappu", Instant.parse("2024-04-30T12:00:00Z"), Instant.parse("2024-05-01T12:00:00Z"));
        when(partyRoster.newestOf(7, Integer.MAX_VALUE)).thenReturn(List.of(ongoing, ended));

        List<PartySummary> parties = tools.listParties();

        assertEquals(List.of(1, 2), parties.stream().map(PartySummary::id).toList());
        assertEquals(false, parties.get(0).ended());
        assertEquals(true, parties.get(1).ended());
    }

    @Test
    void startsAPartyWithTheSignedInUserInItAndALinkToShare() {
        Party party = party(4, "Sauna night", Instant.now(), Instant.now().plus(Duration.ofDays(2)));
        when(partyRoster.start("Sauna night", 2)).thenReturn(party);
        when(joinLinks.regenerate(4, 7)).thenReturn(link(4, "abc"));

        StartedParty started = tools.startParty("Sauna night", 2, null, null, null, null);

        verify(partyRoster).join(4, 7);
        assertEquals(4, started.party().id());
        assertEquals("https://ryyppy.net/join/abc", started.joinLink().url());
        assertNull(started.locationInvite());
        assertNull(started.invited());
        verifyNoInteractions(locationInvites);
    }

    @Test
    void aPartyLastsADayUnlessToldOtherwise() {
        when(partyRoster.start("Sauna night")).thenReturn(party(4, "Sauna night", Instant.now(), Instant.now()));
        when(joinLinks.regenerate(4, 7)).thenReturn(link(4, "abc"));

        tools.startParty("Sauna night", null, null, null, null, null);

        verify(partyRoster).start("Sauna night");
    }

    @Test
    void startsAPartyAtAPlaceAndInvitesPeople() {
        when(partyRoster.start("Cottage")).thenReturn(party(4, "Cottage", Instant.now(), Instant.now()));
        when(joinLinks.regenerate(4, 7)).thenReturn(link(4, "abc"));
        PartyLocationInvite place = invite(4, 62.5, 27.5, 500);
        when(locationInvites.set(4, 62.5, 27.5, 500, 7)).thenReturn(place);
        when(invitationSuggestions.forParty(7, 4, Integer.MAX_VALUE)).thenReturn(List.of(friend(8, "Liisa")));
        when(userAccounts.get(8)).thenReturn(user(8, "Liisa", "liisa@example.com"));

        StartedParty started = tools.startParty("Cottage", null, 62.5, 27.5, 500, List.of(named("Liisa")));

        assertEquals(500, started.locationInvite().radiusM());
        assertEquals(List.of(8), started.invited().invited().stream().map(PartyTools.Invited::userId).toList());
        verify(partyRoster).join(4, 8);
    }

    @Test
    void anInvalidPlaceStartsNoParty() {
        assertThrows(InvalidLocationInviteException.class, () -> tools.startParty("Cottage", null, 95.0, 27.5, 500, null));
        assertThrows(InvalidLocationInviteException.class, () -> tools.startParty("Cottage", null, 62.5, 27.5, 50, null));
        assertThrows(IllegalArgumentException.class, () -> tools.startParty("Cottage", null, 62.5, null, 500, null));

        verifyNoInteractions(partyRoster, joinLinks, locationInvites);
    }

    @Test
    void invitesAFriendByNameWhoIsTheOnlyMatch() {
        members(7);
        when(invitationSuggestions.forParty(7, 5, Integer.MAX_VALUE))
                .thenReturn(List.of(friend(8, "Matti Virtanen"), friend(9, "Liisa Korhonen")));
        when(userAccounts.get(9)).thenReturn(user(9, "Liisa Korhonen", "liisa@example.com"));

        InviteReport report = tools.inviteToParty(5, List.of(named("liisa")));

        verify(partyRoster).join(5, 9);
        assertEquals("Liisa Korhonen", report.invited().get(0).name());
        assertTrue(report.ambiguous().isEmpty() && report.unmatched().isEmpty());
    }

    @Test
    void aNameThatMatchesSeveralFriendsAddsNoneAndReturnsTheCandidates() {
        members(7);
        when(invitationSuggestions.forParty(7, 5, Integer.MAX_VALUE))
                .thenReturn(List.of(friend(8, "Matti Virtanen"), friend(10, "Matti Nieminen"), friend(9, "Liisa")));

        InviteReport report = tools.inviteToParty(5, List.of(named("Matti")));

        verify(partyRoster, never()).join(anyInt(), anyInt());
        assertEquals(List.of(new Candidate(8, "Matti Virtanen"), new Candidate(10, "Matti Nieminen")),
                report.ambiguous().get(0).candidates());
    }

    @Test
    void theClientAnswersAnAmbiguousNameWithTheCandidatesUserId() {
        members(7);
        when(partyAccess.isPartyMate(7, 10)).thenReturn(true);
        when(userAccounts.get(10)).thenReturn(user(10, "Matti Nieminen", "mn@example.com"));

        InviteReport report = tools.inviteToParty(5, List.of(new Person(null, null, 10, null, null)));

        verify(partyRoster).join(5, 10);
        assertEquals(10, report.invited().get(0).userId());
    }

    @Test
    void anIdOfSomeoneTheSignedInUserSharesNoPartyWithAddsNoOne() {
        members(7);

        InviteReport report = tools.inviteToParty(5, List.of(new Person(null, null, 10, null, null)));

        verify(partyRoster, never()).join(anyInt(), anyInt());
        assertEquals(1, report.unmatched().size());
    }

    @Test
    void anUnknownNameIsReportedWithTheClosestFriends() {
        members(7);
        when(invitationSuggestions.forParty(7, 5, Integer.MAX_VALUE))
                .thenReturn(List.of(friend(8, "Matti"), friend(9, "Liisa"), friend(10, "Kalle")));
        when(partyRoster.members(5)).thenReturn(List.of());

        InviteReport report = tools.inviteToParty(5, List.of(named("Lisa")));

        verify(partyRoster, never()).join(anyInt(), anyInt());
        assertEquals("Lisa", report.unmatched().get(0).request());
        assertEquals(new Candidate(9, "Liisa"), report.unmatched().get(0).closest().get(0));
    }

    @Test
    void aFriendAlreadyInThePartyIsReportedAndLeftAlone() {
        members(7);
        when(invitationSuggestions.forParty(7, 5, Integer.MAX_VALUE)).thenReturn(List.of());
        when(partyRoster.members(5)).thenReturn(List.of(user(9, "Liisa", "liisa@example.com")));

        InviteReport report = tools.inviteToParty(5, List.of(named("Liisa")));

        verify(partyRoster, never()).join(anyInt(), anyInt());
        assertTrue(report.invited().get(0).alreadyInParty());
    }

    @Test
    void addsAUserByTheirExactEmail() {
        members(7);
        when(userAccounts.byEmail("make@example.com")).thenReturn(user(8, "Make", "make@example.com"));

        InviteReport report = tools.inviteToParty(5, List.of(new Person(null, "make@example.com", null, null, null)));

        verify(partyRoster).join(5, 8);
        assertEquals(8, report.invited().get(0).userId());
        verifyNoInteractions(invitationSuggestions);
    }

    @Test
    void anUnknownEmailAddsNoOne() {
        members(7);

        InviteReport report = tools.inviteToParty(5, List.of(new Person(null, "nobody@example.com", null, null, null)));

        verify(partyRoster, never()).join(anyInt(), anyInt());
        assertEquals("nobody@example.com", report.unmatched().get(0).request());
    }

    @Test
    void addsAGuestWhoBelongsToThePartyOnly() {
        members(7);
        User saved = user(20, "Veli", null);
        when(userAccounts.add(any())).thenAnswer(call -> {
            User guest = call.getArgument(0);
            guest.setId(20);
            return guest;
        });

        InviteReport report = tools.inviteToParty(5, List.of(new Person("Veli", null, null, User.Sex.MALE, 80f)));

        ArgumentCaptor<User> guest = ArgumentCaptor.forClass(User.class);
        verify(userAccounts).add(guest.capture());
        assertTrue(guest.getValue().isGuest());
        assertEquals(80f, guest.getValue().getWeight());
        verify(partyRoster).join(5, 20);
        assertTrue(report.invited().get(0).guest());
        assertEquals("Veli", saved.getName());
    }

    @Test
    void aGuestWithoutWeightIsNotCreated() {
        members(7);

        InviteReport report = tools.inviteToParty(5, List.of(new Person("Veli", null, null, User.Sex.MALE, null),
                new Person("Pekka", null, null, User.Sex.MALE, 0f)));

        verify(userAccounts, never()).add(any());
        assertEquals(2, report.unmatched().size());
    }

    @Test
    void invitesNoOneToAPartyTheSignedInUserIsNotIn() {
        members();

        assertThrows(AccessDeniedException.class, () -> tools.inviteToParty(5, List.of(named("Liisa"))));

        verifyNoInteractions(partyRoster, invitationSuggestions);
        verify(userAccounts, never()).add(any());
    }

    @Test
    void showsTheCurrentJoinLinkWithoutChangingIt() {
        members(7);
        when(joinLinks.current(5)).thenReturn(Optional.of(link(5, "abc")));
        when(partyRoster.get(5)).thenReturn(party(5, "Sauna", Instant.now(), Instant.now()));
        when(locationInvites.current(5)).thenReturn(Optional.of(invite(5, 62.5, 27.5, 300)));

        PartyInvite shown = tools.getPartyInvite(5, null);

        assertEquals("https://ryyppy.net/join/abc", shown.joinLink().url());
        assertEquals(300, shown.locationInvite().radiusM());
        verify(joinLinks, never()).regenerate(anyInt(), anyInt());
    }

    @Test
    void createsTheJoinLinkOnlyIfThePartyHasNone() {
        members(7);
        when(joinLinks.current(5)).thenReturn(Optional.empty());
        when(joinLinks.regenerate(5, 7)).thenReturn(link(5, "new"));
        when(partyRoster.get(5)).thenReturn(party(5, "Sauna", Instant.now(), Instant.now()));
        when(locationInvites.current(5)).thenReturn(Optional.empty());

        PartyInvite shown = tools.getPartyInvite(5, false);

        assertEquals("https://ryyppy.net/join/new", shown.joinLink().url());
        assertNull(shown.locationInvite());
    }

    @Test
    void replacesTheJoinLinkOnlyWhenAsked() {
        members(7);
        when(joinLinks.regenerate(5, 7)).thenReturn(link(5, "fresh"));
        when(partyRoster.get(5)).thenReturn(party(5, "Sauna", Instant.now(), Instant.now()));
        when(locationInvites.current(5)).thenReturn(Optional.empty());

        assertEquals("https://ryyppy.net/join/fresh", tools.getPartyInvite(5, true).joinLink().url());

        verify(joinLinks, never()).current(anyInt());
    }

    @Test
    void anOutsiderGetsAndReplacesNoInvite() {
        members();

        assertThrows(AccessDeniedException.class, () -> tools.getPartyInvite(5, null));
        assertThrows(AccessDeniedException.class, () -> tools.getPartyInvite(5, true));

        verifyNoInteractions(joinLinks, locationInvites, partyRoster);
    }

    @Test
    void joinsWithTheWholeLinkOrJustItsToken() {
        Party party = party(5, "Sauna", Instant.now(), Instant.now().plus(Duration.ofHours(1)));
        when(joinLinks.join("abc-_9", 7)).thenReturn(party);

        assertEquals(5, tools.joinParty("https://ryyppy.net/join/abc-_9?x=1", null, null, null).joined().id());
        assertEquals(5, tools.joinParty("abc-_9", null, null, null).joined().id());
        assertEquals(5, tools.joinParty(" https://ryyppy.net/join/abc-_9/ ", null, null, null).joined().id());
    }

    @Test
    void anUnusableLinkJoinsNothing() {
        when(joinLinks.join("old", 7)).thenThrow(new UnusableJoinLinkException());

        assertThrows(UnusableJoinLinkException.class, () -> tools.joinParty("old", null, null, null));
    }

    @Test
    void joinsTheOnePartyThatReachesThePosition() {
        Party party = party(5, "Cottage", Instant.now(), Instant.now().plus(Duration.ofHours(1)));
        Position position = new Position(62.5, 27.5);
        when(nearbyParties.around(position)).thenReturn(List.of(new NearbyParty(party, new RoughDistance(300, false))));
        when(nearbyParties.join(5, position, 7)).thenReturn(party);

        JoinOutcome outcome = tools.joinParty(null, 62.5, 27.5, null);

        assertEquals(5, outcome.joined().id());
        verify(nearbyParties).join(5, position, 7);
    }

    @Test
    void severalPartiesAreListedAndNoneJoined() {
        Party first = party(5, "Cottage", Instant.now(), Instant.now().plus(Duration.ofHours(1)));
        Party second = party(6, "Sauna", Instant.now(), Instant.now().plus(Duration.ofHours(1)));
        Position position = new Position(62.5, 27.5);
        when(nearbyParties.around(position)).thenReturn(List.of(new NearbyParty(first, new RoughDistance(100, true)),
                new NearbyParty(second, new RoughDistance(500, false))));
        when(partyAccess.isMember(6, 7)).thenReturn(true);

        JoinOutcome outcome = tools.joinParty(null, 62.5, 27.5, null);

        assertNull(outcome.joined());
        assertEquals(List.of(5, 6), outcome.nearby().stream().map(PartyTools.NearbyPartySummary::id).toList());
        assertEquals(List.of(false, true), outcome.nearby().stream().map(PartyTools.NearbyPartySummary::alreadyMember).toList());
        verify(nearbyParties, never()).join(anyInt(), any(), anyInt());
    }

    @Test
    void joinsTheChosenPartyAtThePosition() {
        Party party = party(6, "Sauna", Instant.now(), Instant.now().plus(Duration.ofHours(1)));
        Position position = new Position(62.5, 27.5);
        when(nearbyParties.join(6, position, 7)).thenReturn(party);

        assertEquals(6, tools.joinParty(null, 62.5, 27.5, 6).joined().id());
    }

    @Test
    void joinNeedsExactlyOneWayToFindTheParty() {
        assertThrows(IllegalArgumentException.class, () -> tools.joinParty(null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> tools.joinParty(null, 62.5, null, null));
        assertThrows(IllegalArgumentException.class, () -> tools.joinParty("abc", 62.5, 27.5, null));

        verifyNoInteractions(joinLinks, nearbyParties);
    }

    @Test
    void findsThePartiesThatReachAPosition() {
        Party party = party(5, "Cottage", Instant.now(), Instant.now().plus(Duration.ofHours(1)));
        when(nearbyParties.around(new Position(62.5, 27.5)))
                .thenReturn(List.of(new NearbyParty(party, new RoughDistance(1500, false))));

        List<PartyTools.NearbyPartySummary> found = tools.findNearbyParties(62.5, 27.5);

        assertEquals(1500, found.get(0).distance().metres());
        assertEquals(false, found.get(0).alreadyMember());
    }

    @Test
    void changesThePartysLengthAndMovesItsLocationInvite() {
        members(7);
        Party party = party(5, "Cottage", Instant.now(), Instant.now().plus(Duration.ofDays(2)));
        when(partyRoster.changeDuration(5, 2)).thenReturn(party);
        when(locationInvites.current(5)).thenReturn(Optional.of(invite(5, 62.1, 27.9, 1000)));

        UpdatedParty updated = tools.updateParty(5, 2, 62.1, 27.9, 1000, null);

        verify(locationInvites).set(5, 62.1, 27.9, 1000, 7);
        assertEquals(1000, updated.locationInvite().radiusM());
    }

    @Test
    void turnsTheLocationInviteOff() {
        members(7);
        when(partyRoster.get(5)).thenReturn(party(5, "Cottage", Instant.now(), Instant.now()));
        when(locationInvites.current(5)).thenReturn(Optional.empty());

        UpdatedParty updated = tools.updateParty(5, null, null, null, null, true);

        verify(locationInvites).revoke(5);
        assertNull(updated.locationInvite());
    }

    @Test
    void anUpdateThatChangesNothingOrContradictsItselfChangesNothing() {
        members(7);

        assertThrows(IllegalArgumentException.class, () -> tools.updateParty(5, null, null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> tools.updateParty(5, 2, 62.1, 27.9, 1000, true));
        assertThrows(InvalidLocationInviteException.class, () -> tools.updateParty(5, 2, 62.1, 27.9, 9000, null));

        verifyNoInteractions(partyRoster, locationInvites);
    }

    @Test
    void anOutsiderUpdatesNothing() {
        members();

        assertThrows(AccessDeniedException.class, () -> tools.updateParty(5, 2, 62.1, 27.9, 1000, null));
        assertThrows(AccessDeniedException.class, () -> tools.updateParty(5, null, null, null, null, true));

        verifyNoInteractions(partyRoster, locationInvites);
    }

    @Test
    void removesAParticipantOfAPartyTheSignedInUserIsIn() {
        members(7, 8);
        when(userAccounts.get(8)).thenReturn(user(8, "Veli", null));

        assertEquals("Veli", tools.removeParticipant(5, 8).name());

        verify(partyRoster).leave(5, 8);
    }

    @Test
    void removesNoOneWhoIsNotInTheParty() {
        members(7);

        assertThrows(IllegalArgumentException.class, () -> tools.removeParticipant(5, 8));

        verifyNoInteractions(partyRoster);
    }

    @Test
    void anOutsiderRemovesNoOne() {
        members(8);

        assertThrows(AccessDeniedException.class, () -> tools.removeParticipant(5, 8));

        verifyNoInteractions(partyRoster);
    }

    private void members(int... userIds) {
        for (int userId : userIds) {
            when(partyAccess.isMember(5, userId)).thenReturn(true);
        }
    }

    private static Person named(String name) {
        return new Person(name, null, null, null, null);
    }

    private static Friend friend(int id, String name) {
        return new Friend(id, name, "http://gravatar");
    }

    private static PartyJoinLink link(int partyId, String token) {
        PartyJoinLink link = new PartyJoinLink();
        link.setPartyId(partyId);
        link.setToken(token);
        link.setCreatedAt(Instant.parse("2026-11-15T20:00:00Z"));
        return link;
    }

    private static PartyLocationInvite invite(int partyId, double latitude, double longitude, int radiusM) {
        PartyLocationInvite invite = new PartyLocationInvite();
        invite.setPartyId(partyId);
        invite.setLatitude(latitude);
        invite.setLongitude(longitude);
        invite.setRadiusM(radiusM);
        return invite;
    }

    private static void signInWithToken(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static User user(int id, String name, String email) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        user.setEmail(email);
        return user;
    }

    private static Party party(int id, String name, Instant start, Instant end) {
        Party party = new Party();
        party.setId(id);
        party.setName(name);
        party.setStartTime(start);
        party.setEndTime(end);
        return party;
    }
}
