package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import drinkcounter.DrinkLog;
import drinkcounter.PartyEndedException;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.PartyAccess;
import drinkcounter.mcp.DrinkTools.DrinkSummary;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class DrinkToolsTest {

    private static final int SELF = 7;
    private static final int MATE = 8;
    private static final int PARTY = 3;

    private final UserAccounts userAccounts = mock(UserAccounts.class);
    private final DrinkLog drinkLog = mock(DrinkLog.class);
    private final PartyAccess partyAccess = mock(PartyAccess.class);
    private final DrinkTools tools = new DrinkTools(userAccounts, drinkLog, partyAccess);

    @BeforeEach
    void signIn() {
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "RS256").subject("matti@example.com").build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        User matti = new User();
        matti.setId(SELF);
        when(userAccounts.byEmail("matti@example.com")).thenReturn(matti);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void logsAStandardDrinkForTheSignedInUser() {
        when(drinkLog.record(eq(SELF), any(), eq(12f))).thenReturn(drink(1, 12f));

        DrinkSummary logged = tools.logDrink(null, null, null, null, null);

        assertEquals(new DrinkSummary(1, SELF, Instant.parse("2026-11-15T20:00:00Z"), 12f), logged);
    }

    @Test
    void logsAtTheGivenTimeWithTheAlcoholOfTheGivenDrink() {
        when(drinkLog.record(eq(SELF), any(), anyFloat())).thenReturn(drink(1, 12f));

        tools.logDrink(null, null, 330f, 4.7f, "2026-11-15T18:00:00Z");

        verify(drinkLog).record(SELF, Date.from(Instant.parse("2026-11-15T18:00:00Z")), 0.33f * 0.047f * 789f);
    }

    @Test
    void logsForAPartyMateThroughASharedParty() {
        members(SELF, MATE);
        when(drinkLog.recordInParty(eq(PARTY), eq(MATE), any(), anyFloat())).thenReturn(drink(2, 12f));

        DrinkSummary logged = tools.logDrink(PARTY, MATE, null, null, null);

        assertEquals(MATE, logged.userId());
    }

    @Test
    void logsForOneselfThroughAParty() {
        members(SELF);
        when(drinkLog.recordInParty(eq(PARTY), eq(SELF), any(), anyFloat())).thenReturn(drink(2, 12f));

        assertEquals(SELF, tools.logDrink(PARTY, null, null, null, null).userId());
    }

    @Test
    void anEndedPartyAcceptsNoDrink() {
        members(SELF, MATE);
        when(drinkLog.recordInParty(eq(PARTY), eq(MATE), any(), anyFloat())).thenThrow(new PartyEndedException(PARTY));

        assertThrows(PartyEndedException.class, () -> tools.logDrink(PARTY, MATE, null, null, null));
    }

    @Test
    void logsNothingForAnotherUserWithoutAParty() {
        assertThrows(AccessDeniedException.class, () -> tools.logDrink(null, MATE, null, null, null));

        verifyNoInteractions(drinkLog);
    }

    @Test
    void logsNothingThroughAPartyTheSignedInUserIsNotIn() {
        members(MATE);

        assertThrows(AccessDeniedException.class, () -> tools.logDrink(PARTY, MATE, null, null, null));
        assertThrows(AccessDeniedException.class, () -> tools.logDrink(PARTY, null, null, null, null));

        verifyNoInteractions(drinkLog);
    }

    @Test
    void logsNothingForSomeoneWhoIsNotInTheParty() {
        members(SELF);

        assertThrows(AccessDeniedException.class, () -> tools.logDrink(PARTY, MATE, null, null, null));

        verifyNoInteractions(drinkLog);
    }

    @Test
    void rejectsAVolumeWithoutAPercentage() {
        assertThrows(IllegalArgumentException.class, () -> tools.logDrink(null, null, 330f, null, null));

        verifyNoInteractions(drinkLog);
    }

    @Test
    void correctsAnOwnDrink() {
        when(drinkLog.correctAlcohol(eq(SELF), eq(5), anyFloat())).thenReturn(drink(5, 20f));

        DrinkSummary corrected = tools.correctDrink(5, 500f, 5f, null, null);

        verify(drinkLog).correctAlcohol(SELF, 5, 0.5f * 0.05f * 789f);
        assertEquals(5, corrected.id());
    }

    @Test
    void correctsADrinkOfAPartyMateThroughASharedParty() {
        members(SELF, MATE);
        when(drinkLog.correctAlcohol(eq(MATE), eq(5), anyFloat())).thenReturn(drink(5, 20f));

        assertEquals(MATE, tools.correctDrink(5, 500f, 5f, PARTY, MATE).userId());
    }

    @Test
    void correctsNothingOfSomeoneOutsideTheSignedInUsersParties() {
        assertThrows(AccessDeniedException.class, () -> tools.correctDrink(5, 500f, 5f, null, MATE));
        members(SELF);
        assertThrows(AccessDeniedException.class, () -> tools.correctDrink(5, 500f, 5f, PARTY, MATE));
        members(MATE);
        assertThrows(AccessDeniedException.class, () -> tools.correctDrink(5, 500f, 5f, PARTY, MATE));

        verifyNoInteractions(drinkLog);
    }

    @Test
    void undoesAnOwnDrink() {
        tools.undoDrink(5, null, null);

        verify(drinkLog).undo(SELF, 5);
    }

    @Test
    void undoesADrinkOfAPartyMateThroughASharedParty() {
        members(SELF, MATE);

        tools.undoDrink(5, PARTY, MATE);

        verify(drinkLog).undo(MATE, 5);
    }

    @Test
    void undoesNothingOfSomeoneOutsideTheSignedInUsersParties() {
        assertThrows(AccessDeniedException.class, () -> tools.undoDrink(5, null, MATE));
        members(SELF);
        assertThrows(AccessDeniedException.class, () -> tools.undoDrink(5, PARTY, MATE));
        members(MATE);
        assertThrows(AccessDeniedException.class, () -> tools.undoDrink(5, PARTY, MATE));

        verify(drinkLog, never()).undo(anyInt(), anyInt());
    }

    private void members(int... userIds) {
        reset(partyAccess);
        for (int userId : userIds) {
            when(partyAccess.isMember(PARTY, userId)).thenReturn(true);
        }
    }

    private static Drink drink(int id, float grams) {
        Drink drink = new Drink();
        drink.setId(id);
        drink.setTimeStamp(Instant.parse("2026-11-15T20:00:00Z"));
        drink.setAlcohol(grams);
        return drink;
    }
}
