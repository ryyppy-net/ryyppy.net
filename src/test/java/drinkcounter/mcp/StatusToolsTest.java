package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import drinkcounter.DrinkLog;
import drinkcounter.PartyRoster;
import drinkcounter.Promilles;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.mcp.StatusTools.MyStatus;
import drinkcounter.mcp.StatusTools.PartyStatus;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.DrinkRepository;
import drinkcounter.web.controllers.api.v2.DrinkDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class StatusToolsTest {

    private final UserAccounts userAccounts = mock(UserAccounts.class);
    private final PartyRoster partyRoster = mock(PartyRoster.class);
    private final PartyAccess partyAccess = mock(PartyAccess.class);
    private final DrinkRepository drinkRepository = mock(DrinkRepository.class);
    private final DrinkLog drinkLog = mock(DrinkLog.class);
    private final StatusTools tools = new StatusTools(userAccounts, partyRoster, partyAccess, new Promilles(drinkRepository), drinkLog);

    private final User matti = user(7, "Matti");

    @BeforeEach
    void signIn() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("matti@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        when(userAccounts.byEmail("matti@example.com")).thenReturn(matti);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myStatusIsReadFromTheSignedInUsersRecentDrinks() {
        when(drinkRepository.findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(anyCollection(), any()))
                .thenReturn(List.of(drink(matti, Instant.now().minus(Duration.ofMinutes(10)))));

        MyStatus status = tools.getMyStatus();

        assertEquals("Matti", status.name());
        assertEquals(1, status.drinksSinceSober());
        assertEquals(true, status.promilles() > 0);
        verify(drinkRepository).findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(eqIds(7), any());
    }

    @Test
    void myDrinksAreTheSignedInUsersOwn() {
        Drink drink = drink(matti, Instant.parse("2026-01-01T20:00:00Z"));
        drink.setId(3);
        when(drinkLog.drinksOf(7)).thenReturn(List.of(drink));

        List<DrinkDTO> drinks = tools.listMyDrinks();

        assertEquals(List.of(3), drinks.stream().map(DrinkDTO::getId).toList());
        assertEquals("2026-01-01T20:00:00Z", drinks.get(0).getTimestamp());
    }

    @Test
    void partyStatusCountsOnlyDrinksLoggedDuringTheParty() {
        Instant start = Instant.now().minus(Duration.ofHours(3));
        Party party = party(5, "Sauna night", start, start.plus(Duration.ofDays(1)));
        User liisa = user(8, "Liisa");
        when(partyAccess.isMember(5, 7)).thenReturn(true);
        when(partyRoster.get(5)).thenReturn(party);
        when(partyRoster.members(5)).thenReturn(List.of(matti, liisa));
        when(drinkRepository.findByDrinkerIdInAndTimeStampBetweenOrderByTimeStampAsc(anyCollection(), any(), any()))
                .thenReturn(List.of(drink(liisa, start.plus(Duration.ofHours(1)))));

        PartyStatus status = tools.getPartyStatus(5);

        assertEquals("Sauna night", status.name());
        assertEquals(false, status.ended());
        assertEquals(List.of("Matti", "Liisa"), status.participants().stream().map(StatusTools.Participant::name).toList());
        assertEquals(0, status.participants().get(0).standardDrinks());
        assertEquals(1, status.participants().get(1).standardDrinks());
        verify(drinkRepository).findByDrinkerIdInAndTimeStampBetweenOrderByTimeStampAsc(anyCollection(), eqInstant(start), any());
        verify(drinkRepository, never()).findByDrinkerIdInAndTimeStampAfterOrderByTimeStampAsc(anyCollection(), any());
    }

    @Test
    void partyStatusOfAPartyTheUserIsNotAMemberOfIsRefusedWithoutReadingIt() {
        when(partyAccess.isMember(5, 7)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> tools.getPartyStatus(5));

        verifyNoInteractions(partyRoster, drinkRepository);
    }

    private static java.util.Collection<Integer> eqIds(int id) {
        return org.mockito.ArgumentMatchers.eq(List.of(id));
    }

    private static Instant eqInstant(Instant instant) {
        return org.mockito.ArgumentMatchers.eq(instant);
    }

    private static Drink drink(User drinker, Instant at) {
        Drink drink = new Drink();
        drink.setDrinker(drinker);
        drink.setTimeStamp(at);
        return drink;
    }

    private static User user(int id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
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
