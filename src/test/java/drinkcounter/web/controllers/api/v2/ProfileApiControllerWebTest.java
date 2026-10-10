package drinkcounter.web.controllers.api.v2;

import com.csvreader.CsvReader;
import drinkcounter.DrinkLog;
import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import jakarta.servlet.ServletException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives ProfileApiController through MockMvc and the app's security filter chain, so the
 * signed-in user reaches the handlers the same way it does in the running app.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class ProfileApiControllerWebTest {

    private static final String DRINK_HISTORY = "/API/v2/profile/drink-history";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DrinkLog drinkLog;

    @Autowired
    private UserService userService;

    @Autowired
    private ProfileApiController controller;

    private User user;
    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        when(userService.getUser(42)).thenReturn(user);

        Drink saved = new Drink();
        saved.setId(7);
        saved.setTimeStamp(Instant.parse("2024-03-05T13:37:42Z"));
        when(drinkLog.record(anyInt(), any(), any())).thenReturn(saved);
    }

    @AfterEach
    public void restoreClockAndTimeZone() {
        ReflectionTestUtils.setField(controller, "clock", Clock.systemUTC());
        if (originalDefaultTimeZone != null) {
            TimeZone.setDefault(originalDefaultTimeZone);
        }
    }

    @Test
    public void profileUpdateIsSavedOnTheSignedInUser() throws Exception {
        mvc.perform(post("/API/v2/profile")
                        .param("name", "Ville").param("email", "ville@example.com")
                        .param("sex", "MALE").param("weight", "80"))
                .andExpect(status().isOk());

        verify(userService).updateUser(user);
        assertEquals("Ville", user.getName());
        assertEquals(80f, user.getWeight());
    }

    @Test
    public void drinkIsAddedForTheSignedInUser() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(drinkLog).record(eq(42), any(), any());
    }

    @Test
    public void drinkParsesIsoTimestampParam() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks").param("timestamp", "2024-03-05T13:37:42.123Z"))
                .andExpect(status().isOk());

        Date expected = Date.from(Instant.parse("2024-03-05T13:37:42.123Z"));
        verify(drinkLog).record(eq(42), eq(expected), any(Float.class));
    }

    @Test
    public void malformedTimestampIsRejectedWithBadRequest() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks").param("timestamp", "not-a-timestamp"))
                .andExpect(status().isBadRequest());

        verify(drinkLog, never()).record(anyInt(), any(), any());
    }

    @Test
    public void changeDrinkUpdatesAlcoholOfOwnDrink() throws Exception {
        mvc.perform(put("/API/v2/profile/drinks/7").param("volume", "0.5").param("alcohol", "0.05"))
                .andExpect(status().isOk());

        verify(drinkLog).correctAlcohol(eq(42), eq(7), any(Float.class));
    }

    @Test
    public void drinkIsDeletedOnlyFromTheSignedInUser() throws Exception {
        mvc.perform(delete("/API/v2/profile/drinks/7"))
                .andExpect(status().isOk());

        verify(drinkLog).undo(42, 7);
    }

    @Test
    public void drinksAreListedWithIsoTimestamps() throws Exception {
        Drink drink = new Drink();
        drink.setId(9);
        drink.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        user.drink(drink);

        mvc.perform(get("/API/v2/profile/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(9))
                .andExpect(jsonPath("$[0].timestamp").value("2024-03-05T13:37:42.123Z"));
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(drinkLog);
        verify(userService, never()).getUser(anyInt());
    }

    @Test
    @WithMockUser
    public void principalThatIsNotDrinkcounterUserDetailsFailsTheRequest() {
        assertThrows(ServletException.class, () -> mvc.perform(delete("/API/v2/profile/drinks/7")));
        assertThrows(ServletException.class, () -> mvc.perform(get("/API/v2/profile/drinks")));

        verifyNoInteractions(drinkLog);
        verify(userService, never()).getUser(anyInt());
    }

    // The history buckets by UTC day, so its "Time" column must be midnight UTC, not
    // midnight in the server's default zone.
    @Test
    public void drinkHistoryTimeColumnIsMidnightUtcNotServerZone() throws Exception {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

        Drink drink = new Drink();
        drink.setTimeStamp(Instant.parse("2024-03-06T01:00:00Z"));
        user.drink(drink);

        long millis = findMillisForUtcDay(drinkHistoryCsv(), "2024-03-06");

        assertEquals(Instant.parse("2024-03-06T00:00:00Z").toEpochMilli(), millis,
                "Time column should be midnight 2024-03-06 UTC, not midnight in the server's zone (America/Los_Angeles)");
    }

    @Test
    public void drinkHistoryTodayBucketIsTheUtcDate() throws Exception {
        originalDefaultTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
        // 2024-03-06T02:00:00Z is still 2024-03-05 in America/Los_Angeles.
        ReflectionTestUtils.setField(controller, "clock",
                Clock.fixed(Instant.parse("2024-03-06T02:00:00Z"), ZoneOffset.UTC));

        Map<String, Integer> countsByDay = parseTimeCountCsv(drinkHistoryCsv());

        assertTrue(countsByDay.containsKey("2024-03-06"),
                "today's bucket should be 2024-03-06 (UTC date), not 2024-03-05 (server's America/Los_Angeles date)");
    }

    @Test
    public void getDrinkHistoryBucketsDrinksByUtcDay() throws Exception {
        Drink drink1 = new Drink();
        drink1.setTimeStamp(Instant.parse("2024-03-05T10:00:00.000Z"));
        Drink drink2 = new Drink();
        drink2.setTimeStamp(Instant.parse("2024-03-06T10:00:00.000Z"));
        user.drink(drink1);
        user.drink(drink2);

        Map<String, Integer> countsByDay = parseTimeCountCsv(drinkHistoryCsv());

        assertEquals(1, countsByDay.getOrDefault("2024-03-05", 0));
        assertEquals(1, countsByDay.getOrDefault("2024-03-06", 0));
        String today = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(countsByDay.containsKey(today), "should always include today's bucket");
    }

    private byte[] drinkHistoryCsv() throws Exception {
        return mvc.perform(get(DRINK_HISTORY))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
    }

    private long findMillisForUtcDay(byte[] csv, String targetDay) throws Exception {
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), StandardCharsets.UTF_8);
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

    private Map<String, Integer> parseTimeCountCsv(byte[] csv) throws Exception {
        Map<String, Integer> result = new HashMap<>();
        CsvReader reader = new CsvReader(new ByteArrayInputStream(csv), StandardCharsets.UTF_8);
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
