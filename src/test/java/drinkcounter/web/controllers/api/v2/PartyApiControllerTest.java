package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PartyApiControllerTest {

    private DrinkCounterService drinkCounterService;
    private PartyApiController controller;
    private User participant;
    private Party party;
    private User outsider;

    @BeforeEach
    public void setUp() {
        drinkCounterService = mock(DrinkCounterService.class);
        UserService userService = mock(UserService.class);
        CurrentUser currentUser = mock(CurrentUser.class);

        participant = new User();
        participant.setId(2);
        outsider = new User();
        outsider.setId(3);
        party = new Party();
        party.setId(1);
        party.addParticipant(participant);

        when(drinkCounterService.getParty(1)).thenReturn(party);
        when(userService.getUser(2)).thenReturn(participant);
        when(userService.getUser(3)).thenReturn(outsider);

        Drink saved = new Drink();
        saved.setId(7);
        saved.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        when(drinkCounterService.addDrink(anyInt(), any(), any())).thenReturn(saved);

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

    @Test
    public void drinkRejectsMalformedTimestamp() {
        assertThrows(DateTimeParseException.class,
                () -> controller.drink(1, 2, null, null, "not-a-timestamp"));
    }

    @Test
    public void handleInvalidTimestampReturnsBadRequest() {
        DateTimeParseException ex = new DateTimeParseException("bad", "not-a-timestamp", 0);

        ResponseEntity<String> response = controller.handleInvalidTimestamp(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void drinkReturnsTheSavedDrink() {
        DrinkDTO dto = controller.drink(1, 2, null, null, null);

        assertEquals(7, dto.getId());
        assertEquals("2024-03-05T13:37:42.123Z", dto.getTimestamp());
    }

    @Test
    public void changeDrinkUpdatesAlcoholOfParticipantsDrink() {
        controller.changeDrink(1, 2, 7, 0.5f, 0.05f);

        verify(drinkCounterService).changeDrinkAlcohol(eq(2), eq(7), any(Float.class));
    }

    @Test
    public void changeDrinkRejectsNonParticipant() {
        assertThrows(RuntimeException.class, () -> controller.changeDrink(1, 3, 7, 0.5f, 0.05f));

        verify(drinkCounterService, never()).changeDrinkAlcohol(anyInt(), anyInt(), any(Float.class));
    }

    @Test
    public void removeDrinkRemovesParticipantsDrink() {
        controller.removeDrink(1, 2, 7);

        verify(drinkCounterService).removeDrinkFromUser(2, 7);
    }

    @Test
    public void removeDrinkRejectsNonParticipant() {
        assertThrows(RuntimeException.class, () -> controller.removeDrink(1, 3, 7));

        verify(drinkCounterService, never()).removeDrinkFromUser(anyInt(), anyInt());
    }
}
