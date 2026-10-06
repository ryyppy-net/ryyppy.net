package drinkcounter.web.controllers.api.v2;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Drink;
import drinkcounter.model.Friend;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import jakarta.servlet.ServletException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives PartyApiController through MockMvc and the app's security filter chain, so the
 * signed-in user reaches the handlers the same way it does in the running app.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class PartyApiControllerWebTest {

    private static final String PARTIES = "/API/v2/parties";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DrinkCounterService drinkCounterService;

    @Autowired
    private UserService userService;

    private User signedIn;
    private User participant;
    private User outsider;
    private Party party;

    @BeforeEach
    public void setUp() {
        signedIn = new User();
        signedIn.setId(42);
        participant = new User();
        participant.setId(2);
        outsider = new User();
        outsider.setId(3);
        party = new Party();
        party.setId(1);
        party.setName("Sauna");
        party.setStartTime(Instant.parse("2024-03-05T12:00:00Z"));
        party.addParticipant(participant);

        when(userService.getUser(42)).thenReturn(signedIn);
        when(userService.getUser(2)).thenReturn(participant);
        when(userService.getUser(3)).thenReturn(outsider);
        when(drinkCounterService.getParty(1)).thenReturn(party);
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(true);

        Drink saved = new Drink();
        saved.setId(7);
        saved.setTimeStamp(Instant.parse("2024-03-05T13:37:42.123Z"));
        when(drinkCounterService.addDrink(anyInt(), any(), any())).thenReturn(saved);
    }

    @Test
    public void partiesAreListedForTheSignedInUser() throws Exception {
        signedIn.setParties(List.of(party));

        mvc.perform(get(PARTIES))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Sauna"))
                .andExpect(jsonPath("$[0].participants.length()").value(1));
    }

    @Test
    public void startedPartyIsLinkedToTheSignedInUser() throws Exception {
        Party started = new Party();
        started.setId(5);
        started.setName("Mökki");
        started.setStartTime(Instant.parse("2024-03-05T12:00:00Z"));
        when(drinkCounterService.startParty("Mökki")).thenReturn(started);

        mvc.perform(post(PARTIES).param("name", "Mökki"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));

        verify(drinkCounterService).linkUserToParty(42, 5);
    }

    @Test
    public void invitationSuggestionsAreForTheSignedInUser() throws Exception {
        when(drinkCounterService.suggestInvitations(42, 1, 10)).thenReturn(List.of(new Friend(9, "Ville", "ville@example.com")));

        mvc.perform(get(PARTIES + "/1/invitations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        verify(drinkCounterService).suggestInvitations(42, 1, 10);
    }

    @Test
    public void drinkParsesIsoTimestampParam() throws Exception {
        mvc.perform(post(PARTIES + "/1/participants/2/drinks").param("timestamp", "2024-03-05T13:37:42.123Z"))
                .andExpect(status().isOk());

        Date expected = Date.from(Instant.parse("2024-03-05T13:37:42.123Z"));
        verify(drinkCounterService).addDrink(eq(2), eq(expected), any(Float.class));
    }

    @Test
    public void drinkDefaultsToNowWhenTimestampMissing() throws Exception {
        mvc.perform(post(PARTIES + "/1/participants/2/drinks"))
                .andExpect(status().isOk());

        verify(drinkCounterService).addDrink(eq(2), eq((Date) null), any(Float.class));
    }

    @Test
    public void malformedTimestampIsRejectedWithBadRequest() throws Exception {
        mvc.perform(post(PARTIES + "/1/participants/2/drinks").param("timestamp", "not-a-timestamp"))
                .andExpect(status().isBadRequest());

        verify(drinkCounterService, never()).addDrink(anyInt(), any(), any());
    }

    @Test
    public void drinkReturnsTheSavedDrink() throws Exception {
        mvc.perform(post(PARTIES + "/1/participants/2/drinks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.timestamp").value("2024-03-05T13:37:42.123Z"));
    }

    @Test
    public void getParticipantRejectsNonParticipantNamingBoth() {
        ServletException ex = assertThrows(ServletException.class,
                () -> mvc.perform(get(PARTIES + "/1/participants/3")));

        assertEquals("Participant 3 doesn't belong to party 1", ex.getCause().getMessage());
    }

    @Test
    public void changeDrinkUpdatesAlcoholOfParticipantsDrink() throws Exception {
        mvc.perform(put(PARTIES + "/1/participants/2/drinks/7").param("volume", "0.5").param("alcohol", "0.05"))
                .andExpect(status().isOk());

        verify(drinkCounterService).changeDrinkAlcohol(eq(2), eq(7), any(Float.class));
    }

    @Test
    public void changeDrinkRejectsNonParticipant() {
        ServletException ex = assertThrows(ServletException.class, () -> mvc.perform(
                put(PARTIES + "/1/participants/3/drinks/7").param("volume", "0.5").param("alcohol", "0.05")));
        assertEquals("Participant 3 doesn't belong to party 1", ex.getCause().getMessage());

        verify(drinkCounterService, never()).changeDrinkAlcohol(anyInt(), anyInt(), any(Float.class));
    }

    @Test
    public void removeDrinkRemovesParticipantsDrink() throws Exception {
        mvc.perform(delete(PARTIES + "/1/participants/2/drinks/7"))
                .andExpect(status().isOk());

        verify(drinkCounterService).removeDrinkFromUser(2, 7);
    }

    @Test
    public void removeDrinkRejectsNonParticipant() {
        assertThrows(ServletException.class, () -> mvc.perform(delete(PARTIES + "/1/participants/3/drinks/7")));

        verify(drinkCounterService, never()).removeDrinkFromUser(anyInt(), anyInt());
    }

    @Test
    public void memberGetsTheParty() throws Exception {
        mvc.perform(get(PARTIES + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Sauna"));
    }

    @Test
    public void memberGetsTheParticipants() throws Exception {
        mvc.perform(get(PARTIES + "/1/participants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(2));
    }

    @Test
    public void memberGetsAParticipant() throws Exception {
        mvc.perform(get(PARTIES + "/1/participants/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    public void memberAddsAGuestParticipant() throws Exception {
        when(userService.addUser(any(User.class))).thenAnswer(invocation -> {
            User guest = invocation.getArgument(0);
            guest.setId(8);
            return guest;
        });

        mvc.perform(post(PARTIES + "/1/participants").param("name", "Vieras").param("sex", "FEMALE").param("weight", "60"))
                .andExpect(status().isOk());

        verify(drinkCounterService).linkUserToParty(8, 1);
    }

    @Test
    public void memberAddsAParticipantByEmail() throws Exception {
        when(userService.getUserByEmail("outsider@example.com")).thenReturn(outsider);

        mvc.perform(post(PARTIES + "/1/participants").param("email", "outsider@example.com"))
                .andExpect(status().isOk());

        verify(drinkCounterService).linkUserToParty(3, 1);
    }

    @Test
    public void memberRemovesAParticipant() throws Exception {
        mvc.perform(delete(PARTIES + "/1/participants/2"))
                .andExpect(status().isOk());

        verify(drinkCounterService).unlinkUserFromParty(2, 1);
    }

    @Test
    public void memberInvitesAnyUser() throws Exception {
        mvc.perform(post(PARTIES + "/1/invitations").param("userId", "3"))
                .andExpect(status().isOk());

        verify(drinkCounterService).linkUserToParty(3, 1);
    }

    static Stream<Arguments> partyRequests() {
        return Stream.of(
                Arguments.of("get party", get(PARTIES + "/1")),
                Arguments.of("get participants", get(PARTIES + "/1/participants")),
                Arguments.of("add guest", post(PARTIES + "/1/participants").param("name", "Vieras").param("sex", "MALE").param("weight", "80")),
                Arguments.of("add participant by email", post(PARTIES + "/1/participants").param("email", "user@example.com")),
                Arguments.of("remove participant", delete(PARTIES + "/1/participants/2")),
                Arguments.of("get participant", get(PARTIES + "/1/participants/2")),
                Arguments.of("add drink", post(PARTIES + "/1/participants/2/drinks")),
                Arguments.of("change drink", put(PARTIES + "/1/participants/2/drinks/7").param("volume", "0.5").param("alcohol", "0.05")),
                Arguments.of("remove drink", delete(PARTIES + "/1/participants/2/drinks/7")),
                Arguments.of("suggest invitations", get(PARTIES + "/1/invitations")),
                Arguments.of("invite", post(PARTIES + "/1/invitations").param("userId", "42")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("partyRequests")
    public void outsiderIsForbiddenAndChangesNothing(String name, MockHttpServletRequestBuilder request) throws Exception {
        when(drinkCounterService.isUserParticipant(1, 42)).thenReturn(false);

        mvc.perform(request).andExpect(status().isForbidden());

        verify(drinkCounterService).isUserParticipant(1, 42);
        verifyNoMoreInteractions(drinkCounterService);
        verifyNoInteractions(userService);
    }

    @Test
    @WithAnonymousUser
    public void anonymousPartyRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get(PARTIES + "/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(drinkCounterService);
    }

    @Test
    @WithAnonymousUser
    public void anonymousRequestIsSentToTheLoginPage() throws Exception {
        mvc.perform(get(PARTIES))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login"));

        verifyNoInteractions(drinkCounterService);
        verify(userService, never()).getUser(anyInt());
    }
}
