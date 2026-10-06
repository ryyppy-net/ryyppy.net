package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/** Drives PartyController through MockMvc and the app's security filter chain. */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class PartyControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DrinkCounterService drinkCounterService;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthenticationChecks authenticationChecks;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        when(userService.getUser(42)).thenReturn(user);
    }

    @Test
    public void partyPageShowsThePartyAndTheSignedInUser() throws Exception {
        Party party = new Party();
        party.setId(5);
        when(drinkCounterService.getParty(5)).thenReturn(party);

        mvc.perform(get("/ui/party").param("id", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("party"))
                .andExpect(model().attribute("party", party))
                .andExpect(model().attribute("user", user));

        verify(authenticationChecks).checkRightsForParty(5);
    }

    @Test
    public void addPartyStartsAndLinksThePartyThenRedirectsToIt() throws Exception {
        Party party = new Party();
        party.setId(8);
        when(drinkCounterService.startParty("Sauna")).thenReturn(party);

        mvc.perform(get("/ui/addParty").param("name", "Sauna").param("userId", "42"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("party?id=8"));

        verify(authenticationChecks).checkLowLevelRightsToUser(42);
        verify(drinkCounterService).linkUserToParty(42, 8);
    }

    @Test
    public void removeUserFromPartyUnlinksAndRedirectsToUserPage() throws Exception {
        mvc.perform(get("/ui/removeUserFromParty").param("partyId", "5").param("userId", "42"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(authenticationChecks).checkRightsForParty(5);
        verify(authenticationChecks).checkHighLevelRightsToUser(42);
        verify(drinkCounterService).unlinkUserFromParty(42, 5);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get("/ui/party").param("id", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(drinkCounterService);
        verify(userService, never()).getUser(anyInt());
    }
}
