package drinkcounter.web.controllers.api.v2;

import com.csvreader.CsvReader;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ProfileApiControllerTest {

    private DrinkCounterService drinkCounterService;
    private User user;
    private ProfileApiController controller;

    @BeforeEach
    public void setUp() {
        drinkCounterService = mock(DrinkCounterService.class);
        UserService userService = mock(UserService.class);
        CurrentUser currentUser = mock(CurrentUser.class);

        user = new User();
        user.setId(1);
        when(currentUser.getUser()).thenReturn(user);

        controller = new ProfileApiController(drinkCounterService, userService, currentUser);
    }

    @Test
    public void drinkParsesIsoTimestampParam() {
        controller.drink(null, null, "2024-03-05T13:37:42.123Z");

        Date expected = Date.from(Instant.parse("2024-03-05T13:37:42.123Z"));
        verify(drinkCounterService).addDrink(eq(1), eq(expected), any(Float.class));
    }

    @Test
    public void drinkRejectsMalformedTimestamp() {
        assertThrows(DateTimeParseException.class,
                () -> controller.drink(null, null, "not-a-timestamp"));
    }

    @Test
    public void handleInvalidTimestampReturnsBadRequest() {
        DateTimeParseException ex = new DateTimeParseException("bad", "not-a-timestamp", 0);

        ResponseEntity<String> response = controller.handleInvalidTimestamp(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    @Test
    public void getDrinksReportsIsoTimestamp() {
        Drink drink = new Drink();
        drink.setId(9);
        drink.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        user.drink(drink);

        List<DrinkDTO> dtos = controller.getDrinks();

        assertEquals(1, dtos.size());
        assertEquals("2024-03-05T13:37:42.123Z", dtos.get(0).getTimestamp());
    }

    @Test
    public void getDrinkHistoryBucketsDrinksByUtcDay() throws Exception {
        Drink drink1 = new Drink();
        drink1.setTimeStamp(Instant.parse("2024-03-05T10:00:00.000Z"));
        Drink drink2 = new Drink();
        drink2.setTimeStamp(Instant.parse("2024-03-06T10:00:00.000Z"));
        user.drink(drink1);
        user.drink(drink2);

        ResponseEntity<byte[]> response = controller.getDrinkHistory();

        Map<String, Integer> countsByDay = parseTimeCountCsv(response.getBody());
        assertEquals(1, countsByDay.getOrDefault("2024-03-05", 0));
        assertEquals(1, countsByDay.getOrDefault("2024-03-06", 0));

        String today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(countsByDay.containsKey(today), "should always include today's bucket");
    }

    private Map<String, Integer> parseTimeCountCsv(byte[] csv) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), java.nio.charset.StandardCharsets.UTF_8);
        reader.readHeaders();
        while (reader.readRecord()) {
            long millis = Long.parseLong(reader.get(0));
            int count = Integer.parseInt(reader.get(1));
            String day = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            result.put(day, count);
        }
        reader.close();
        return result;
    }
}
