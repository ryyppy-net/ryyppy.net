package drinkcounter.web;

import drinkcounter.DrinkLog;
import drinkcounter.PartyRoster;
import drinkcounter.PassphraseLogin;
import drinkcounter.UserAccounts;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.repository.PartyRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The session-cookie chain refuses an unsafe request that does not echo the {@code XSRF-TOKEN} cookie in
 * {@code X-XSRF-TOKEN}. Unlike the other controller tests, requests here carry no token of their own.
 */
@ControllerWebTest
@WithDrinkcounterUser(userId = 42)
public class CsrfProtectionWebTest {

    private static final String TOKEN = CsrfTokenConfiguration.TOKEN;

    private MockMvc mvc;

    @Autowired
    private DrinkLog drinkLog;

    @Autowired
    private UserAccounts userAccounts;

    @Autowired
    private PassphraseLogin passphraseLogin;

    @Autowired
    private PartyRoster partyRoster;

    @Autowired
    private PartyRepository partyRepository;

    @BeforeEach
    public void setUp(@Autowired WebApplicationContext context) {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();

        User user = new User();
        user.setId(42);
        when(userAccounts.get(42)).thenReturn(user);
        Drink saved = new Drink();
        saved.setId(7);
        saved.setTimeStamp(Instant.parse("2024-03-05T13:37:42Z"));
        when(drinkLog.record(anyInt(), any(), any())).thenReturn(saved);
    }

    @Test
    public void postWithoutATokenIsForbiddenAndChangesNothing() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drinkLog);
    }

    @Test
    public void postWithTheTokenCookieEchoedInTheHeaderIsAccepted() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks")
                        .cookie(new Cookie("XSRF-TOKEN", TOKEN))
                        .header("X-XSRF-TOKEN", TOKEN))
                .andExpect(status().isOk());

        verify(drinkLog).record(anyInt(), any(), any());
    }

    @Test
    public void postWithAHeaderThatDiffersFromTheCookieIsForbidden() throws Exception {
        mvc.perform(post("/API/v2/profile/drinks")
                        .cookie(new Cookie("XSRF-TOKEN", TOKEN))
                        .header("X-XSRF-TOKEN", "someone-elses-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(drinkLog);
    }

    @Test
    public void tokenCookieIsSetOnTheFirstPageLoad() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/app/")).andReturn().getResponse();

        assertThat(tokenCookies(response)).singleElement().asString().doesNotContain("HttpOnly");
    }

    @Test
    public void tokenCookieIsSetOnTheClassicPartyPage() throws Exception {
        Party party = new Party();
        party.setId(5);
        when(partyRoster.get(5)).thenReturn(party);
        when(partyRoster.members(5)).thenReturn(List.of(new User()));
        when(partyRepository.countUserParticipations(5, 42)).thenReturn(1L);

        MockHttpServletResponse response = mvc.perform(get("/ui/party").param("id", "5")).andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(tokenCookies(response)).hasSize(1);
    }

    @Test
    @WithAnonymousUser
    public void tokenCookieIsSetOnTheLoginPage() throws Exception {
        MockHttpServletResponse response = mvc.perform(get("/ui/login")).andExpect(status().isOk())
                .andReturn().getResponse();

        assertThat(tokenCookies(response)).hasSize(1);
    }

    @Test
    @WithAnonymousUser
    public void formLoginWithoutATokenIsForbidden() throws Exception {
        mvc.perform(post("/login").param("username", "user@example.com").param("password", "secret"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithAnonymousUser
    public void registrationWithoutATokenIsForbidden() throws Exception {
        mvc.perform(post("/ui/addUser").param("name", "Ville"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userAccounts);
    }

    @Test
    public void logoutWithoutATokenIsForbidden() throws Exception {
        mvc.perform(post("/logout"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void logoutWithATokenSignsOut() throws Exception {
        mvc.perform(post("/logout").with(CsrfTokenConfiguration.TOKEN_COOKIE_AND_HEADER))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login?logout"));
    }

    @Test
    @WithAnonymousUser
    public void passphraseEndpointsTakeNoToken() throws Exception {
        mvc.perform(post("/API/passphrase/some-passphrase/undo-drink"));

        verify(passphraseLogin).findUser("some-passphrase");
    }

    private static List<String> tokenCookies(MockHttpServletResponse response) {
        return response.getHeaders(HttpHeaders.SET_COOKIE).stream().filter(c -> c.startsWith("XSRF-TOKEN=")).toList();
    }
}
