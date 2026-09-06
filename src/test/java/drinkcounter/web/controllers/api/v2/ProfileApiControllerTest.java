package drinkcounter.web.controllers.api.v2;

import com.csvreader.CsvReader;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
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

    private TimeZone originalDefaultTimeZone;

    @AfterEach
    public void restoreDefaultTimeZone() {
        if (originalDefaultTimeZone != null) {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    // Issue #55: the bucketing offset here is currently hardcoded to 0 (UTC)
    // -- see the commented-out session read in getDrinkHistory -- so the
    // "Time" column must represent midnight UTC, not midnight in whatever
    // zone the JVM happens to default to.
    @Test
    public void getDrinkHistoryTimeColumnRepresentsMidnightUtc_issue55() throws Exception {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        Drink drink = new Drink();
        drink.setTimeStamp(Instant.parse("2024-03-06T01:00:00Z")); // 2024-03-06 in UTC
        user.drink(drink);

        ResponseEntity<byte[]> response = controller.getDrinkHistory();

        long millis = findMillisForUtcDay(response.getBody(), "2024-03-06");

        long utcMidnightMillis = Instant.parse("2024-03-06T00:00:00Z").toEpochMilli();
        assertEquals(utcMidnightMillis, millis,
                "Time column should be midnight 2024-03-06 UTC, not midnight in the server's zone (America/Los_Angeles)");
    }

    // Issue #55: "today" must be decided in the same zone the bucketing
    // uses (currently hardcoded UTC), not the server's systemDefault().
    @Test
    public void getDrinkHistoryTodayBucketUsesUtc_issue55() throws Exception {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles")); // UTC-8

        // 2024-03-06T02:00:00Z is already 2024-03-05 in America/Los_Angeles.
        Clock fixedClock = Clock.fixed(Instant.parse("2024-03-06T02:00:00Z"), ZoneOffset.UTC);
        ReflectionTestUtils.setField(controller, "clock", fixedClock);

        // no drinks: only the "today" bucket will be present

        ResponseEntity<byte[]> response = controller.getDrinkHistory();

        Map<String, Integer> countsByDay = parseTimeCountCsv(response.getBody());

        assertTrue(countsByDay.containsKey("2024-03-06"),
                "today's bucket should be 2024-03-06 (UTC date), not 2024-03-05 (server's America/Los_Angeles date)");
    }

    private long findMillisForUtcDay(byte[] csv, String targetDay) throws Exception {
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), java.nio.charset.StandardCharsets.UTF_8);
        reader.readHeaders();
        long result = -1;
        while (reader.readRecord()) {
            long millis = Long.parseLong(reader.get(0));
            String dayUtc = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString();
            if (dayUtc.equals(targetDay)) {
                result = millis;
            }
        }
        reader.close();
        if (result == -1) {
            throw new AssertionError("No row found for UTC day " + targetDay);
        }
        return result;
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

        // The bucketing offset here is hardcoded to UTC, so the "Time"
        // column is only meaningful when decoded in UTC too -- not the
        // server's systemDefault(), which is fixed separately (see
        // getDrinkHistoryTimeColumnRepresentsMidnightUtc_issue55).
        Map<String, Integer> countsByDay = parseTimeCountCsv(response.getBody());
        assertEquals(1, countsByDay.getOrDefault("2024-03-05", 0));
        assertEquals(1, countsByDay.getOrDefault("2024-03-06", 0));

        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(countsByDay.containsKey(today), "should always include today's bucket");
    }

    private Map<String, Integer> parseTimeCountCsv(byte[] csv) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), java.nio.charset.StandardCharsets.UTF_8);
        reader.readHeaders();
        while (reader.readRecord()) {
            long millis = Long.parseLong(reader.get(0));
            int count = Integer.parseInt(reader.get(1));
            String day = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            result.put(day, count);
        }
        reader.close();
        return result;
    }
}
