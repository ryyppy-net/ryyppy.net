package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkLog;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import jakarta.servlet.ServletException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives ProfileApiController through MockMvc and the app's security filter chain, so the
 * signed-in user reaches the handlers the same way it does in the running app.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class ProfileApiControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DrinkLog drinkLog;

    @Autowired
    private UserAccounts userAccounts;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        when(userAccounts.get(42)).thenReturn(user);

        Drink saved = new Drink();
        saved.setId(7);
        saved.setTimeStamp(Instant.parse("2024-03-05T13:37:42Z"));
        when(drinkLog.record(anyInt(), any(), any())).thenReturn(saved);
    }

    @Test
    public void profileIsJsonWhateverTheBrowserPrefers() throws Exception {
        mvc.perform(get("/API/v2/profile").header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    public void profileUpdateIsSavedOnTheSignedInUser() throws Exception {
        mvc.perform(post("/API/v2/profile")
                        .param("name", "Ville").param("email", "ville@example.com")
                        .param("sex", "MALE").param("weight", "80"))
                .andExpect(status().isOk());

        verify(userAccounts).update(user);
        assertEquals("Ville", user.getName());
        assertEquals(80f, user.getWeight());
    }

    @Test
    public void drinkIsAddedForTheSignedInUser() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drink.id").value(7));

        verify(drinkLog).record(eq(42), any(), any());
    }

    @Test
    public void addedDrinkComesBackWithWhatTheDashboardShows() throws Exception {
        Drink earlier = new Drink();
        earlier.setId(6);
        earlier.setTimeStamp(Instant.parse("2024-03-05T12:00:00Z"));
        user.drink(earlier);

        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.id").value(42))
                .andExpect(jsonPath("$.profile.totalDrinks").exists())
                .andExpect(jsonPath("$.profile.history").isArray())
                .andExpect(jsonPath("$.drink.timestamp").value("2024-03-05T13:37:42Z"))
                .andExpect(jsonPath("$.latestDrinks").doesNotExist())
                .andExpect(jsonPath("$.drinkHistory").doesNotExist());
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
        Drink changed = new Drink();
        changed.setId(7);
        changed.setTimeStamp(Instant.parse("2024-03-05T13:37:42Z"));
        when(drinkLog.correctAlcohol(eq(42), eq(7), any(Float.class))).thenReturn(changed);

        mvc.perform(put("/API/v2/profile/drinks/7").param("volume", "0.5").param("alcohol", "0.05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drink.id").value(7))
                .andExpect(jsonPath("$.profile.id").value(42));

        verify(drinkLog).correctAlcohol(eq(42), eq(7), any(Float.class));
    }

    @Test
    public void drinkIsDeletedOnlyFromTheSignedInUser() throws Exception {
        mvc.perform(delete("/API/v2/profile/drinks/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.drink").doesNotExist())
                .andExpect(jsonPath("$.profile.id").value(42));

        verify(drinkLog).undo(42, 7);
    }

    @Test
    public void drinksAreListedWithIsoTimestamps() throws Exception {
        Drink drink = new Drink();
        drink.setId(9);
        drink.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        user.drink(drink);
        when(drinkLog.drinksOf(42)).thenReturn(List.of(drink));

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
        verify(userAccounts, never()).get(anyInt());
    }

    @Test
    @WithMockUser
    public void principalThatIsNotDrinkcounterUserDetailsFailsTheRequest() {
        assertThrows(ServletException.class, () -> mvc.perform(delete("/API/v2/profile/drinks/7")));
        assertThrows(ServletException.class, () -> mvc.perform(get("/API/v2/profile/drinks")));

        verifyNoInteractions(drinkLog);
        verify(userAccounts, never()).get(anyInt());
    }
}
