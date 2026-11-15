package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import drinkcounter.PartyRoster;
import drinkcounter.UserAccounts;
import drinkcounter.mcp.PartyTools.PartySummary;
import drinkcounter.model.Party;
import drinkcounter.model.User;
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
    private final PartyTools tools = new PartyTools(userAccounts, partyRoster);

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
