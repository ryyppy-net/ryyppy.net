package drinkcounter.web.controllers;

import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Drink;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class DefaultControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        user.setName("Ville");
        user.setParties(new ArrayList<>());
        when(userService.getUser(42)).thenReturn(user);
    }

    @Test
    public void appIndexEmbedsTheSignedInUsersInitialData() throws Exception {
        Drink drink = new Drink();
        drink.setId(9);
        drink.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        user.drink(drink);

        mvc.perform(get("/app/index.html"))
                .andExpect(status().isOk())
                .andExpect(view().name("app/index"))
                .andExpect(model().attribute("initialProfile", containsString("Ville")))
                .andExpect(model().attribute("initialParties", "[]"))
                .andExpect(model().attribute("initialDrinks", containsString("2024-03-05T13:37:42.123Z")))
                .andExpect(model().attribute("initialDrinkHistory", containsString("Time")))
                .andExpect(model().attributeExists("templates"));

        verify(userService).getUser(42);
    }

    @Test
    public void rootRedirectsToTheFrontPage() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/app/index.html#/"));
    }

    @Test
    @WithAnonymousUser
    public void anonymousAppIndexRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get("/app/index.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verify(userService, never()).getUser(anyInt());
    }
}
