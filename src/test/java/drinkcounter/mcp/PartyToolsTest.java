package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import drinkcounter.InvitationSuggestions;
import drinkcounter.PartyJoinLinks;
import drinkcounter.PartyRoster;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.mcp.PartyTools.Added;
import drinkcounter.mcp.PartyTools.Invitee;
import drinkcounter.mcp.PartyTools.JoinLink;
import drinkcounter.mcp.PartyTools.PartySummary;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.PartyJoinLink;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.security.access.AccessDeniedException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class PartyToolsTest {

    private final UserAccounts userAccounts = mock(UserAccounts.class);
    private final PartyRoster partyRoster = mock(PartyRoster.class);
    private final PartyAccess partyAccess = mock(PartyAccess.class);
    private final InvitationSuggestions invitationSuggestions = mock(InvitationSuggestions.class);
    private final PartyJoinLinks joinLinks = mock(PartyJoinLinks.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final PartyTools tools = new PartyTools(userAccounts, partyRoster, partyAccess, invitationSuggestions,
            joinLinks, request);

    @BeforeEach
    void signIn() {
        signInWithToken("matti@example.com");
        when(userAccounts.byEmail("matti@example.com")).thenReturn(user(7, "matti@example.com"));
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
        signInWithToken("matti@example.com");
        User matti = user(7, "matti@example.com");
        when(userAccounts.byEmail("matti@example.com")).thenReturn(matti);
        Party ongoing = party(1, "Sauna night", Instant.now().minus(Duration.ofHours(2)), Instant.now().plus(Duration.ofHours(22)));
        Party ended = party(2, "Vappu", Instant.parse("2024-04-30T12:00:00Z"), Instant.parse("2024-05-01T12:00:00Z"));
        when(partyRoster.newestOf(7, Integer.MAX_VALUE)).thenReturn(List.of(ongoing, ended));

        List<PartySummary> parties = tools.listParties();

        assertEquals(List.of(1, 2), parties.stream().map(PartySummary::id).toList());
        assertEquals("Sauna night", parties.get(0).name());
        assertEquals(false, parties.get(0).ended());
        assertEquals(true, parties.get(1).ended());
        assertEquals(Instant.parse("2024-05-01T12:00:00Z"), parties.get(1).endTime());
    }

    @Test
    void startsAPartyAndJoinsTheSignedInUserToIt() {
        Party party = party(4, "Sauna night", Instant.now(), Instant.now().plus(Duration.ofDays(2)));
        when(partyRoster.start("Sauna night", 2)).thenReturn(party);

        PartySummary started = tools.startParty("Sauna night", 2);

        verify(partyRoster).join(4, 7);
        assertEquals(4, started.id());
        assertEquals(false, started.ended());
    }

    @Test
    void aPartyLastsADayUnlessToldOtherwise() {
        when(partyRoster.start("Sauna night")).thenReturn(party(4, "Sauna night", Instant.now(), Instant.now()));

        tools.startParty("Sauna night", null);

        verify(partyRoster).start("Sauna night");
    }

    @Test
    void suggestsFriendsToAMember() {
        members(7);
        when(invitationSuggestions.forParty(7, 5, 10)).thenReturn(List.of(new Friend(8, "Make", "http://gravatar")));

        assertEquals(List.of(new Invitee(8, "Make")), tools.suggestInvitees(5, null));
    }

    @Test
    void suggestsNothingToAnOutsider() {
        members();

        assertThrows(AccessDeniedException.class, () -> tools.suggestInvitees(5, null));

        verifyNoInteractions(invitationSuggestions);
    }

    @Test
    void invitesAPartyMate() {
        members(7);
        when(partyAccess.isPartyMate(7, 8)).thenReturn(true);
        when(userAccounts.get(8)).thenReturn(user(8, "make@example.com"));

        Added added = tools.inviteFriend(5, 8);

        verify(partyRoster).join(5, 8);
        assertEquals(8, added.userId());
    }

    @Test
    void invitesNoOneToAPartyTheSignedInUserIsNotIn() {
        members();
        when(partyAccess.isPartyMate(7, 8)).thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> tools.inviteFriend(5, 8));

        verifyNoInteractions(partyRoster);
    }

    @Test
    void invitesNoOneTheSignedInUserSharesNoPartyWith() {
        members(7);

        assertThrows(AccessDeniedException.class, () -> tools.inviteFriend(5, 8));

        verifyNoInteractions(partyRoster);
    }

    @Test
    void addsAUserByEmailToAPartyTheyAreIn() {
        members(7);
        when(userAccounts.byEmail("make@example.com")).thenReturn(user(8, "make@example.com"));

        tools.addUserByEmail(5, "make@example.com");

        verify(partyRoster).join(5, 8);
    }

    @Test
    void addsNoOneByEmailToAPartyTheSignedInUserIsNotIn() {
        members();

        assertThrows(AccessDeniedException.class, () -> tools.addUserByEmail(5, "make@example.com"));

        verifyNoInteractions(partyRoster);
    }

    @Test
    void anUnknownEmailAddsNoOne() {
        members(7);

        assertThrows(IllegalArgumentException.class, () -> tools.addUserByEmail(5, "nobody@example.com"));

        verifyNoInteractions(partyRoster);
    }

    @Test
    void showsTheCurrentJoinLinkWithoutChangingIt() {
        members(7);
        when(joinLinks.current(5)).thenReturn(Optional.of(link(5, "abc")));

        JoinLink shown = tools.getJoinLink(5);

        assertEquals("https://ryyppy.net/join/abc", shown.url());
        verify(joinLinks).current(5);
        org.mockito.Mockito.verifyNoMoreInteractions(joinLinks);
    }

    @Test
    void aPartyWithoutALinkHasNothingToShow() {
        members(7);
        when(joinLinks.current(5)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> tools.getJoinLink(5));
    }

    @Test
    void replacesTheJoinLinkOfAPartyTheSignedInUserIsIn() {
        members(7);
        when(joinLinks.regenerate(5, 7)).thenReturn(link(5, "new"));

        assertEquals("https://ryyppy.net/join/new", tools.createJoinLink(5).url());
    }

    @Test
    void anOutsiderSeesAndReplacesNoJoinLink() {
        members();

        assertThrows(AccessDeniedException.class, () -> tools.getJoinLink(5));
        assertThrows(AccessDeniedException.class, () -> tools.createJoinLink(5));

        verifyNoInteractions(joinLinks);
    }

    private void members(int... userIds) {
        for (int userId : userIds) {
            when(partyAccess.isMember(5, userId)).thenReturn(true);
        }
    }

    private static PartyJoinLink link(int partyId, String token) {
        PartyJoinLink link = new PartyJoinLink();
        link.setPartyId(partyId);
        link.setToken(token);
        link.setCreatedAt(Instant.parse("2026-11-15T20:00:00Z"));
        return link;
    }

    private static void signInWithToken(String subject) {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject(subject).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    private static User user(int id, String email) {
        User user = new User();
        user.setId(id);
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
