package drinkcounter.web.controllers.api;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.User;
import drinkcounter.util.PartyMarshaller;
import drinkcounter.web.ControllerWebTest;
import drinkcounter.web.controllers.ui.AuthenticationController;
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

import static drinkcounter.web.controllers.api.ClassicApiRequests.addDrink;
import static drinkcounter.web.controllers.api.ClassicApiRequests.addGuest;
import static drinkcounter.web.controllers.api.ClassicApiRequests.drinksPerDay;
import static drinkcounter.web.controllers.api.ClassicApiRequests.editDrink;
import static drinkcounter.web.controllers.api.ClassicApiRequests.linkUser;
import static drinkcounter.web.controllers.api.ClassicApiRequests.passphraseAddDrink;
import static drinkcounter.web.controllers.api.ClassicApiRequests.passphraseInfo;
import static drinkcounter.web.controllers.api.ClassicApiRequests.passphraseUndoDrink;
import static drinkcounter.web.controllers.api.ClassicApiRequests.partyXml;
import static drinkcounter.web.controllers.api.ClassicApiRequests.removeDrink;
import static drinkcounter.web.controllers.api.ClassicApiRequests.showDrinks;
import static drinkcounter.web.controllers.api.ClassicApiRequests.showHistory;
import static drinkcounter.web.controllers.api.ClassicApiRequests.userXml;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the classic /API controller, used by the jQuery UI, through MockMvc and the app's
 * security filter chain. The signed-in user is 42; party 1 and user 7 belong to someone else.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class APIControllerWebTest {

    private static final int SIGNED_IN = 42;
    private static final int OTHER_USER = 7;
    private static final int PARTY = 1;
    private static final int DRINK = 5;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private DrinkCounterService drinkCounterService;

    @Autowired
    private UserService userService;

    @Autowired
    private PartyMarshaller partyMarshaller;

    @BeforeEach
    public void setUp() {
        when(userService.getUser(SIGNED_IN)).thenReturn(drinker(SIGNED_IN));
        when(userService.getUser(OTHER_USER)).thenReturn(drinker(OTHER_USER));
    }

    static Stream<Arguments> outsiderRequests() {
        return Stream.of(
                Arguments.of("party", partyXml(PARTY), Rule.PARTY_MEMBER),
                Arguments.of("add guest to party", addGuest(PARTY), Rule.PARTY_MEMBER),
                Arguments.of("link user to party", linkUser(PARTY, SIGNED_IN), Rule.PARTY_MEMBER),
                Arguments.of("show drinks", showDrinks(OTHER_USER), Rule.OWN_USER),
                Arguments.of("drinks per day", drinksPerDay(OTHER_USER), Rule.OWN_USER),
                Arguments.of("user", userXml(OTHER_USER), Rule.OWN_USER_OR_PARTY_MATE),
                Arguments.of("add drink", addDrink(OTHER_USER), Rule.OWN_USER_OR_PARTY_MATE),
                Arguments.of("edit drink", editDrink(OTHER_USER, DRINK), Rule.OWN_USER_OR_PARTY_MATE),
                Arguments.of("remove drink", removeDrink(OTHER_USER, DRINK), Rule.OWN_USER_OR_PARTY_MATE),
                Arguments.of("show history", showHistory(OTHER_USER), Rule.OWN_USER_OR_PARTY_MATE));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("outsiderRequests")
    public void outsiderIsForbiddenAndChangesNothing(String name, MockHttpServletRequestBuilder request, Rule rule)
            throws Exception {
        mvc.perform(request).andExpect(status().isForbidden());

        switch (rule) {
            case PARTY_MEMBER -> verify(drinkCounterService).isUserParticipant(PARTY, SIGNED_IN);
            case OWN_USER_OR_PARTY_MATE -> verify(drinkCounterService).shareParty(SIGNED_IN, OTHER_USER);
            case OWN_USER -> { }
        }
        verifyNoMoreInteractions(drinkCounterService);
        verifyNoInteractions(userService, partyMarshaller);
    }

    static Stream<Arguments> passphraseRequests() {
        return Stream.of(
                Arguments.of("info", passphraseInfo("unknown")),
                Arguments.of("add drink", passphraseAddDrink("unknown")),
                Arguments.of("undo drink", passphraseUndoDrink("unknown")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("passphraseRequests")
    @WithAnonymousUser
    public void unknownPassphraseIsForbiddenAndChangesNothing(String name, MockHttpServletRequestBuilder request)
            throws Exception {
        mvc.perform(request).andExpect(status().isForbidden());

        verify(userService).getUserByPassphrase("unknown");
        verifyNoMoreInteractions(userService);
        verifyNoInteractions(drinkCounterService);
    }

    static Stream<Arguments> partyMateRequests() {
        return Stream.of(
                Arguments.of("user", userXml(OTHER_USER)),
                Arguments.of("add drink", addDrink(OTHER_USER)),
                Arguments.of("edit drink", editDrink(OTHER_USER, DRINK)),
                Arguments.of("remove drink", removeDrink(OTHER_USER, DRINK)),
                Arguments.of("show history", showHistory(OTHER_USER)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("partyMateRequests")
    public void partyMateIsAllowed(String name, MockHttpServletRequestBuilder request) throws Exception {
        when(drinkCounterService.shareParty(SIGNED_IN, OTHER_USER)).thenReturn(true);

        mvc.perform(request).andExpect(status().isOk());
    }

    static Stream<Arguments> ownOnlyRequests() {
        return Stream.of(
                Arguments.of("show drinks", showDrinks(OTHER_USER)),
                Arguments.of("drinks per day", drinksPerDay(OTHER_USER)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ownOnlyRequests")
    public void partyMateIsForbiddenFromTheFullDrinkLog(String name, MockHttpServletRequestBuilder request) throws Exception {
        when(drinkCounterService.shareParty(SIGNED_IN, OTHER_USER)).thenReturn(true);

        mvc.perform(request).andExpect(status().isForbidden());

        verifyNoInteractions(drinkCounterService, userService, partyMarshaller);
    }

    static Stream<Arguments> ownUserRequests() {
        return Stream.of(
                Arguments.of("show drinks", showDrinks(SIGNED_IN)),
                Arguments.of("drinks per day", drinksPerDay(SIGNED_IN)),
                Arguments.of("user", userXml(SIGNED_IN)),
                Arguments.of("add drink", addDrink(SIGNED_IN)),
                Arguments.of("edit drink", editDrink(SIGNED_IN, DRINK)),
                Arguments.of("remove drink", removeDrink(SIGNED_IN, DRINK)),
                Arguments.of("show history", showHistory(SIGNED_IN)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ownUserRequests")
    public void ownUserIsAllowed(String name, MockHttpServletRequestBuilder request) throws Exception {
        mvc.perform(request).andExpect(status().isOk());
    }

    @Test
    public void partyMateAddsADrinkForTheOtherUser() throws Exception {
        when(drinkCounterService.shareParty(SIGNED_IN, OTHER_USER)).thenReturn(true);
        when(drinkCounterService.addDrink(OTHER_USER)).thenReturn(9);

        mvc.perform(addDrink(OTHER_USER))
                .andExpect(status().isOk())
                .andExpect(content().string("9"));
    }

    @Test
    public void partyMateEditsTheOtherUsersDrink() throws Exception {
        when(drinkCounterService.shareParty(SIGNED_IN, OTHER_USER)).thenReturn(true);

        mvc.perform(editDrink(OTHER_USER, DRINK)).andExpect(status().isOk());

        verify(drinkCounterService).changeDrinkAlcohol(eq(OTHER_USER), eq(DRINK), anyFloat());
    }

    @Test
    public void partyMateRemovesTheOtherUsersDrink() throws Exception {
        when(drinkCounterService.shareParty(SIGNED_IN, OTHER_USER)).thenReturn(true);

        mvc.perform(removeDrink(OTHER_USER, DRINK)).andExpect(status().isOk());

        verify(drinkCounterService).removeDrinkFromUser(OTHER_USER, DRINK);
    }

    @Test
    public void memberGetsTheParty() throws Exception {
        when(drinkCounterService.isUserParticipant(PARTY, SIGNED_IN)).thenReturn(true);

        mvc.perform(partyXml(PARTY)).andExpect(status().isOk());

        verify(partyMarshaller).marshall(eq(PARTY), any());
    }

    @Test
    public void memberAddsAGuest() throws Exception {
        when(drinkCounterService.isUserParticipant(PARTY, SIGNED_IN)).thenReturn(true);
        when(userService.addUser(any(User.class))).thenAnswer(invocation -> {
            User guest = invocation.getArgument(0);
            guest.setId(8);
            return guest;
        });

        mvc.perform(addGuest(PARTY))
                .andExpect(status().isOk())
                .andExpect(content().string("8"));

        verify(drinkCounterService).linkUserToParty(8, PARTY);
    }

    @Test
    public void memberLinksAUserToTheParty() throws Exception {
        when(drinkCounterService.isUserParticipant(PARTY, SIGNED_IN)).thenReturn(true);

        mvc.perform(linkUser(PARTY, OTHER_USER))
                .andExpect(status().isOk());

        verify(drinkCounterService).linkUserToParty(OTHER_USER, PARTY);
    }

    @Test
    public void ownUserGetsTheirDrinks() throws Exception {
        mvc.perform(showDrinks(SIGNED_IN)).andExpect(status().isOk());

        verify(partyMarshaller).marshallDrinks(eq(SIGNED_IN), any());
        verifyNoInteractions(drinkCounterService);
    }

    @Test
    public void drinksPerDayHonoursTheOffsetStoredInTheSession() throws Exception {
        mvc.perform(drinksPerDay(SIGNED_IN).sessionAttr(AuthenticationController.TIMEZONEOFFSET, -120.0))
                .andExpect(status().isOk());
    }

    private static User drinker(int id) {
        User user = new User();
        user.setId(id);
        user.setWeight(80);
        user.setSex(User.Sex.MALE);
        return user;
    }

    private enum Rule { PARTY_MEMBER, OWN_USER, OWN_USER_OR_PARTY_MATE }
}
