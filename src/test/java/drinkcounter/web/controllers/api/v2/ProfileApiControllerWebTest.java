package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives ProfileApiController through MockMvc and the app's security filter chain, so the
 * signed-in user reaches the handlers the same way it does in the running app.
 */
@ControllerWebTest
public class ProfileApiControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DrinkCounterService drinkCounterService;

    @Autowired
    private UserService userService;

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void drinkIsAddedForTheSignedInUser() throws Exception {
        Drink saved = new Drink();
        saved.setId(7);
        saved.setTimeStamp(Instant.parse("2024-03-05T13:37:42Z"));
        when(drinkCounterService.addDrink(eq(42), any(), any())).thenReturn(saved);

        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));

        verify(drinkCounterService).addDrink(eq(42), any(), any());
    }

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void drinksAreListedForTheSignedInUser() throws Exception {
        User user = new User();
        user.setId(42);
        Drink drink = new Drink();
        drink.setId(9);
        drink.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        user.drink(drink);
        when(userService.getUser(42)).thenReturn(user);

        mvc.perform(get("/API/v2/profile/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(9));
    }

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void drinkIsDeletedOnlyFromTheSignedInUser() throws Exception {
        mvc.perform(delete("/API/v2/profile/drinks/7"))
                .andExpect(status().isOk());

        verify(drinkCounterService).removeDrinkFromUser(42, 7);
    }

    @Test
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(drinkCounterService);
        verify(userService, never()).getUser(any(Integer.class));
    }
}
