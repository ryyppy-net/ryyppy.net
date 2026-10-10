package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.dao.PartyDAO;
import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import drinkcounter.web.ControllerWebTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Drives the signed-in user handlers of UserController through MockMvc and the app's
 * security filter chain, including the access rule of each user action.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class UserControllerWebTest {

    private static final String DATE = "01.01.2024 12:00";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserService userService;

    @Autowired
    private DrinkCounterService drinkCounterService;

    @Autowired
    private PartyDAO partyDAO;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        when(userService.getUser(42)).thenReturn(user);
    }

    @Test
    public void userPageShowsTheSignedInUserWithPartiesNewestFirst() throws Exception {
        Party older = party("2024-01-01T00:00:00Z");
        Party newer = party("2024-02-01T00:00:00Z");
        user.setParties(new ArrayList<>(List.of(older, newer)));

        mvc.perform(get("/ui/user"))
                .andExpect(status().isOk())
                .andExpect(view().name("user"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("parties", List.of(newer, older)));
    }

    @Test
    public void passphrasePageShowsThePassphraseOfTheSignedInUser() throws Exception {
        user.setPassphrase("correct horse");

        mvc.perform(get("/ui/passphrase"))
                .andExpect(status().isOk())
                .andExpect(view().name("passphrase"))
                .andExpect(model().attribute("passphrase", "correct horse"));
    }

    @Test
    public void passphraseIsGeneratedForTheSignedInUser() throws Exception {
        mvc.perform(get("/ui/passphrase-generate"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("passphrase"));

        verify(userService).generatePassphrase(user);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestsAreSentToTheLoginPage() throws Exception {
        for (String path : List.of("/ui/user", "/ui/passphrase", "/ui/passphrase-generate")) {
            mvc.perform(get(path))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/ui/login"));
        }

        verify(userService, never()).getUser(42);
        verify(userService, never()).generatePassphrase(any());
    }

    @Test
    public void modifyUserChangesTheSignedInUser() throws Exception {
        user.setEmail("me@example.com");

        mvc.perform(modifyUser(42))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(userService).updateUser(user);
        assertEquals("New name", user.getName());
    }

    @Test
    public void modifyUserIsForbiddenForAnotherUserEvenInASharedParty() throws Exception {
        when(partyDAO.countSharedParties(42, 7)).thenReturn(1L);

        mvc.perform(modifyUser(7)).andExpect(status().isForbidden());

        verify(userService, never()).getUser(7);
        verify(userService, never()).updateUser(any());
        verifyNoInteractions(drinkCounterService);
    }

    @Test
    public void addDrinkToDateAddsADrinkForTheSignedInUser() throws Exception {
        mvc.perform(addDrinkToDate(42))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(drinkCounterService).addDrinkToDate(42, DATE, 0.0);
    }

    @Test
    public void addDrinkToDateUsesTheOffsetStoredInTheSession() throws Exception {
        mvc.perform(addDrinkToDate(42).sessionAttr(AuthenticationController.TIMEZONEOFFSET, -120.0))
                .andExpect(status().is3xxRedirection());

        verify(drinkCounterService).addDrinkToDate(42, DATE, -120.0);
    }

    @Test
    public void addDrinkToDateAddsADrinkForAPartyMate()throws Exception {
        when(partyDAO.countSharedParties(42, 7)).thenReturn(1L);

        mvc.perform(addDrinkToDate(7))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(partyDAO).countSharedParties(42, 7);
        verify(drinkCounterService).addDrinkToDate(7, DATE, 0.0);
    }

    @Test
    public void addDrinkToDateIsForbiddenForAnOutsider() throws Exception {
        mvc.perform(addDrinkToDate(7)).andExpect(status().isForbidden());

        verify(partyDAO).countSharedParties(42, 7);
        verifyNoMoreInteractions(drinkCounterService);
    }

    @Test
    public void removeDrinkRemovesADrinkOfTheSignedInUser() throws Exception {
        mvc.perform(removeDrink(42))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(drinkCounterService).removeDrinkFromUser(42, 5);
    }

    @Test
    public void removeDrinkIsForbiddenForAnotherUserEvenInASharedParty() throws Exception {
        when(partyDAO.countSharedParties(42, 7)).thenReturn(1L);

        mvc.perform(removeDrink(7)).andExpect(status().isForbidden());

        verifyNoInteractions(drinkCounterService);
    }

    @Test
    public void getUserByEmailAnswersAMemberOfTheParty() throws Exception {
        User invitee = new User();
        invitee.setId(9);
        when(partyDAO.countUserParticipations(3, 42)).thenReturn(1L);
        when(userService.emailIsCorrect("friend@example.com")).thenReturn(true);
        when(userService.getUserByEmail("friend@example.com")).thenReturn(invitee);

        mvc.perform(getUserByEmail(3))
                .andExpect(status().isOk())
                .andExpect(content().string("9"));
    }

    @Test
    public void getUserByEmailIsForbiddenForAnOutsiderOfTheParty() throws Exception {
        mvc.perform(getUserByEmail(3)).andExpect(status().isForbidden());

        verify(partyDAO).countUserParticipations(3, 42);
        verifyNoMoreInteractions(drinkCounterService);
        verify(userService, never()).getUserByEmail(any());
    }

    @Test
    public void nonNumericIdsAreBadRequests() throws Exception {
        mvc.perform(modifyUser("abc")).andExpect(status().isBadRequest());
        mvc.perform(addDrinkToDate("abc")).andExpect(status().isBadRequest());
        mvc.perform(removeDrink("abc")).andExpect(status().isBadRequest());
        mvc.perform(getUserByEmail("abc")).andExpect(status().isBadRequest());

        verifyNoInteractions(drinkCounterService);
        verify(userService, never()).updateUser(any());
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestsToTheUserActionsAreSentToTheLoginPage() throws Exception {
        for (var request : List.of(modifyUser(42), addDrinkToDate(42), removeDrink(42), getUserByEmail(3))) {
            mvc.perform(request)
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/ui/login"));
        }

        verifyNoInteractions(drinkCounterService);
        verify(userService, never()).updateUser(any());
    }

    private MockHttpServletRequestBuilder modifyUser(Object userId) {
        return post("/ui/modifyUser")
                .param("userId", userId.toString())
                .param("name", "New name")
                .param("sex", "MALE")
                .param("weight", "80")
                .param("email", "me@example.com");
    }

    private MockHttpServletRequestBuilder addDrinkToDate(Object userId) {
        return post("/ui/addDrinkToDate")
                .param("userId", userId.toString())
                .param("date", DATE);
    }

    private MockHttpServletRequestBuilder removeDrink(Object userId) {
        return get("/ui/removeDrink")
                .param("userId", userId.toString())
                .param("drinkId", "5");
    }

    private MockHttpServletRequestBuilder getUserByEmail(Object partyId) {
        return get("/ui/getUserByEmail")
                .param("partyId", partyId.toString())
                .param("email", "friend@example.com");
    }

    private Party party(String start) {
        Party party = new Party();
        party.setStartTime(Instant.parse(start));
        return party;
    }
}
