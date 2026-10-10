package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.repository.PartyRepository;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private PartyRepository partyRepository;

    @Autowired
    private UserAccounts userAccounts;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        when(userAccounts.get(42)).thenReturn(user);
    }

    @Test
    public void partyPageShowsThePartyAndTheSignedInUser() throws Exception {
        Party party = new Party();
        party.setId(5);
        when(drinkCounterService.getParty(5)).thenReturn(party);
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(1L);

        mvc.perform(get("/ui/party").param("id", "5"))
                .andExpect(status().isOk())
                .andExpect(view().name("party"))
                .andExpect(model().attribute("party", party))
                .andExpect(model().attribute("user", user));

        verify(partyRepository).countUserParticipations(5, 42);
    }

    @Test
    public void addPartyStartsAndLinksThePartyThenRedirectsToIt() throws Exception {
        Party party = new Party();
        party.setId(8);
        when(drinkCounterService.startParty("Sauna")).thenReturn(party);

        mvc.perform(get("/ui/addParty").param("name", "Sauna").param("userId", "42"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("party?id=8"));

        verify(drinkCounterService).linkUserToParty(42, 8);
    }

    @Test
    public void removeUserFromPartyUnlinksYourselfAndRedirectsToUserPage() throws Exception {
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(1L);

        mvc.perform(removeUserFromParty(5, 42))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(drinkCounterService).unlinkUserFromParty(42, 5);
    }

    @Test
    public void removeUserFromPartyUnlinksAPartyMate() throws Exception {
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(1L);
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        mvc.perform(removeUserFromParty(5, 7))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("user"));

        verify(drinkCounterService).unlinkUserFromParty(7, 5);
    }

    @Test
    public void partyPageIsForbiddenToAnOutsider() throws Exception {
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(0L);

        mvc.perform(get("/ui/party").param("id", "5"))
                .andExpect(status().isForbidden());

        verify(partyRepository).countUserParticipations(5, 42);
        verifyNoMoreInteractions(drinkCounterService);
    }

    @Test
    public void addPartyIsForbiddenForAnotherUser() throws Exception {
        mvc.perform(get("/ui/addParty").param("name", "Sauna").param("userId", "7"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drinkCounterService);
    }

    @Test
    public void addPartyIsForbiddenForAPartyMate() throws Exception {
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        mvc.perform(get("/ui/addParty").param("name", "Sauna").param("userId", "7"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drinkCounterService);
    }

    @Test
    public void removeUserFromPartyIsForbiddenToAnOutsiderOfTheParty() throws Exception {
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(0L);
        when(partyRepository.countSharedParties(42, 7)).thenReturn(1L);

        mvc.perform(removeUserFromParty(5, 7))
                .andExpect(status().isForbidden());

        verify(partyRepository).countUserParticipations(5, 42);
        verifyNoMoreInteractions(drinkCounterService);
    }

    @Test
    public void removeUserFromPartyIsForbiddenToAnOutsiderRemovingThemself() throws Exception {
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(0L);

        mvc.perform(removeUserFromParty(5, 42))
                .andExpect(status().isForbidden());

        verify(partyRepository).countUserParticipations(5, 42);
        verifyNoMoreInteractions(drinkCounterService);
    }

    @Test
    public void removeUserFromPartyIsForbiddenForAUserWhoSharesNoPartyWithTheMember() throws Exception {
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(1L);
        when(partyRepository.countSharedParties(42, 7)).thenReturn(0L);

        mvc.perform(removeUserFromParty(5, 7))
                .andExpect(status().isForbidden());

        verify(partyRepository).countUserParticipations(5, 42);
        verify(partyRepository).countSharedParties(42, 7);
        verifyNoMoreInteractions(drinkCounterService);
    }

    @Test
    public void nonNumericIdsAreRejectedBeforeAnyAccessCheck() throws Exception {
        mvc.perform(get("/ui/party").param("id", "abc")).andExpect(status().isBadRequest());
        mvc.perform(get("/ui/addParty").param("name", "Sauna").param("userId", "abc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/ui/removeUserFromParty").param("partyId", "abc").param("userId", "42"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/ui/removeUserFromParty").param("partyId", "5").param("userId", "abc"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(drinkCounterService);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get("/ui/party").param("id", "5"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(drinkCounterService);
        verify(userAccounts, never()).get(anyInt());
    }

    private static MockHttpServletRequestBuilder removeUserFromParty(int partyId, int userId) {
        return get("/ui/removeUserFromParty").param("partyId", String.valueOf(partyId))
                .param("userId", String.valueOf(userId));
    }
}
