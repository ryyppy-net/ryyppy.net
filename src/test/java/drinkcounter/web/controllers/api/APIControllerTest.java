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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    @Test
    public void drinkHistoryBucketsDrinksByClientLocalDay() throws Exception {
        // Client timezone offset is JS-style: UTC+02:00 is reported as -120.
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

        Map<String, Integer> countsByDay = parseTimeCountCsv(response.getBody());

        assertEquals(1, countsByDay.getOrDefault("2024-03-05", 0));
        assertEquals(1, countsByDay.getOrDefault("2024-03-06", 0));

        String today = LocalDate.now(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
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
