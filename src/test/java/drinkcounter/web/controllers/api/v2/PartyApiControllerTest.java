package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartyApiControllerTest {

    private DrinkCounterService drinkCounterService;
    private PartyApiController controller;
    private User participant;
    private Party party;

    @BeforeEach
    public void setUp() {
        drinkCounterService = mock(DrinkCounterService.class);
        UserService userService = mock(UserService.class);
        CurrentUser currentUser = mock(CurrentUser.class);

        participant = new User();
        participant.setId(2);
        party = new Party();
        party.setId(1);
        party.addParticipant(participant);

        when(drinkCounterService.getParty(1)).thenReturn(party);
        when(userService.getUser(2)).thenReturn(participant);

        controller = new PartyApiController(currentUser, drinkCounterService, userService);
    }

    @Test
    public void drinkParsesIsoTimestampParam() {
        controller.drink(1, 2, null, null, "2024-03-05T13:37:42.123Z");

        Date expected = Date.from(Instant.parse("2024-03-05T13:37:42.123Z"));
        verify(drinkCounterService).addDrink(eq(2), eq(expected), any(Float.class));
    }

    @Test
    public void drinkDefaultsToNowWhenTimestampMissing() {
        controller.drink(1, 2, null, null, null);

        verify(drinkCounterService).addDrink(eq(2), eq((Date) null), any(Float.class));
    }
}
