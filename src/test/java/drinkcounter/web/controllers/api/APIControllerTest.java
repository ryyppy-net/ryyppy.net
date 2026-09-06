package drinkcounter.web.controllers.api;

import com.csvreader.CsvReader;
import drinkcounter.AlcoholServiceImpl;
import drinkcounter.UserService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.web.controllers.ui.AuthenticationController;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class APIControllerTest {

    private APIController controller;
    private UserService userService;
    private HttpSession session;

    @BeforeEach
    public void setUp() {
        AlcoholServiceImpl.getInstance().reset();

        controller = new APIController();
        userService = mock(UserService.class);
        AuthenticationChecks authenticationChecks = mock(AuthenticationChecks.class);
        session = mock(HttpSession.class);

        ReflectionTestUtils.setField(controller, "userService", userService);
        ReflectionTestUtils.setField(controller, "authenticationChecks", authenticationChecks);
    }

    private TimeZone originalDefaultTimeZone;

    @AfterEach
    public void restoreDefaultTimeZone() {
        if (originalDefaultTimeZone != null) {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    // Issue #55: the CSV "Time" column for a day bucket must represent
    // midnight in the CLIENT's zone (the same zone used to decide which
    // bucket the drink belongs to), not the server's JVM zone.
    @Test
    public void drinkHistoryTimeColumnRepresentsMidnightInClientZone_issue55() throws Exception {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        // Client reports UTC+2 using JS convention (offset = -120).
        when(session.getAttribute(AuthenticationController.TIMEZONEOFFSET)).thenReturn(-120.0);

        User user = new User();
        user.setId(1);

        Drink drink = new Drink();
        drink.setTimeStamp(Instant.parse("2024-03-05T22:30:00Z")); // 2024-03-06T00:30 in client zone
        user.drink(drink);

        when(userService.getUser(1)).thenReturn(user);

        ResponseEntity<byte[]> response = controller.drinkHistory(session, "1");

        long millis = findMillisForClientDay(response.getBody(), ZoneOffset.ofHours(2), "2024-03-06");

        long clientMidnightMillis = Instant.parse("2024-03-05T22:00:00Z").toEpochMilli();
        assertEquals(clientMidnightMillis, millis,
                "Time column should be midnight 2024-03-06 in the CLIENT's zone (UTC+2), "
                + "not the server's zone (America/Los_Angeles)");
    }

    private long findMillisForClientDay(byte[] csv, ZoneOffset clientZone, String targetDay) throws Exception {
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), java.nio.charset.StandardCharsets.UTF_8);
        reader.readHeaders();
        long result = -1;
        while (reader.readRecord()) {
            long millis = Long.parseLong(reader.get(0));
            String dayInClientZone = Instant.ofEpochMilli(millis).atZone(clientZone).toLocalDate().toString();
            if (dayInClientZone.equals(targetDay)) {
                result = millis;
            }
        }
        reader.close();
        if (result == -1) {
            throw new AssertionError("No row found for day " + targetDay + " in client zone " + clientZone);
        }
        return result;
    }

    // Issue #55: "today" must always be decided in the CLIENT's zone,
    // not the server's. Client at UTC+14 (Kiritimati); a fixed instant
    // that is already the next calendar day there while UTC is still on
    // the previous day.
    @Test
    public void drinkHistoryTodayBucketUsesClientZone_issue55() throws Exception {
        when(session.getAttribute(AuthenticationController.TIMEZONEOFFSET)).thenReturn(-840.0); // UTC+14

        Clock fixedClock = Clock.fixed(Instant.parse("2024-03-05T23:00:00Z"), ZoneOffset.UTC);
        ReflectionTestUtils.setField(controller, "clock", fixedClock);

        User user = new User();
        user.setId(1); // no drinks: only the "today" bucket will be present

        when(userService.getUser(1)).thenReturn(user);

        ResponseEntity<byte[]> response = controller.drinkHistory(session, "1");

        Map<String, Integer> countsByDay = parseTimeCountCsv(response.getBody(), ZoneOffset.ofHours(14));

        assertTrue(countsByDay.containsKey("2024-03-06"),
                "today's bucket should be 2024-03-06 (client's UTC+14 date), not 2024-03-05 (UTC/server date)");
    }

    @Test
    public void drinkHistoryBucketsDrinksByClientLocalDay() throws Exception {
        // Client timezone offset is JS-style: UTC+02:00 is reported as -120.
        ZoneOffset clientZone = ZoneOffset.ofHours(2);
        when(session.getAttribute(AuthenticationController.TIMEZONEOFFSET)).thenReturn(-120.0);

        User user = new User();
        user.setId(1);

        Drink beforeMidnightLocal = new Drink();
        beforeMidnightLocal.setTimeStamp(Instant.parse("2024-03-05T21:00:00Z")); // 2024-03-05T23:00 local
        Drink afterMidnightLocal = new Drink();
        afterMidnightLocal.setTimeStamp(Instant.parse("2024-03-05T23:00:00Z")); // 2024-03-06T01:00 local
        user.drink(beforeMidnightLocal);
        user.drink(afterMidnightLocal);

        when(userService.getUser(1)).thenReturn(user);

        ResponseEntity<byte[]> response = controller.drinkHistory(session, "1");

        // The "Time" column is only meaningful when decoded in the same zone
        // the server used to produce it: the client's zone (dtz), not the
        // server's own systemDefault().
        Map<String, Integer> countsByDay = parseTimeCountCsv(response.getBody(), clientZone);

        assertEquals(1, countsByDay.getOrDefault("2024-03-05", 0));
        assertEquals(1, countsByDay.getOrDefault("2024-03-06", 0));

        String today = LocalDate.now(clientZone).format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(countsByDay.containsKey(today), "should always include today's bucket");
    }

    @Test
    public void showHistoryReportsPromilleSlopeOverLast300Minutes() throws Exception {
        User user = new User();
        user.setId(1);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);

        Drink drink = new Drink();
        drink.setTimeStamp(Instant.now().minus(Duration.ofMinutes(200)));
        user.drink(drink);

        when(userService.getUser(1)).thenReturn(user);

        long before = System.currentTimeMillis();
        ResponseEntity<byte[]> response = controller.showHistory(session, "1");
        long after = System.currentTimeMillis();

        List<long[]> rows = parseTimeValueCsv(response.getBody());
        assertTrue(rows.size() > 1);

        long windowMillis = Duration.ofMinutes(300).toMillis();
        long firstTimestamp = rows.get(0)[0];
        assertTrue(Math.abs(firstTimestamp - (before - windowMillis)) < 5000,
                "first point should be ~300 minutes before now, was " + firstTimestamp);

        long lastTimestamp = rows.get(rows.size() - 1)[0];
        assertTrue(lastTimestamp >= before - 5000 && lastTimestamp <= after + 5000,
                "last point should be ~now, was " + lastTimestamp);
    }

    private Map<String, Integer> parseTimeCountCsv(byte[] csv, ZoneOffset decodeZone) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), java.nio.charset.StandardCharsets.UTF_8);
        reader.readHeaders();
        while (reader.readRecord()) {
            long millis = Long.parseLong(reader.get(0));
            int count = Integer.parseInt(reader.get(1));
            String day = Instant.ofEpochMilli(millis).atZone(decodeZone).toLocalDate()
                    .format(DateTimeFormatter.ISO_LOCAL_DATE);
            result.put(day, count);
        }
        reader.close();
        return result;
    }

    private List<long[]> parseTimeValueCsv(byte[] csv) throws Exception {
        List<long[]> result = new ArrayList<>();
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), java.nio.charset.StandardCharsets.UTF_8);
        reader.readHeaders();
        while (reader.readRecord()) {
            result.add(new long[]{Long.parseLong(reader.get(0))});
        }
        reader.close();
        return result;
    }
}
