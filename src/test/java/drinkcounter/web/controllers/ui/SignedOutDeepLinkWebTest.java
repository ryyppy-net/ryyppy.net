package drinkcounter.web.controllers.ui;

import drinkcounter.UserAccounts;
import drinkcounter.authentication.GoogleIdentityLinkingService;
import drinkcounter.authentication.relay.AuthRelayTokenService;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

/**
 * A visitor opens a page signed out, signs in one of the app's ways, and is taken on to that page,
 * as Spring Security's saved request gives it.
 * No user annotation: the session's own security context has to carry each request.
 */
@ControllerWebTest
public class SignedOutDeepLinkWebTest {

    private static final String EMAIL = "visitor@example.com";
    private static final String PASSWORD = "correct-horse-battery-staple";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UserAccounts userAccounts;

    @Autowired
    private GoogleIdentityLinkingService identityLinkingService;

    @Autowired
    private AuthRelayTokenService relayTokenService;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    private User user;

    @BeforeEach
    public void setUp() {
        user = new User();
        user.setId(42);
        user.setName("Vierailija");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setParties(new ArrayList<>());
        when(userAccounts.get(42)).thenReturn(user);
    }

    private MockHttpSession openSignedOut(String url) throws Exception {
        return (MockHttpSession) mvc.perform(get(url))
                .andExpect(redirectedUrl("/ui/login"))
                .andReturn().getRequest().getSession(false);
    }

    // As a browser's session cookie does; Spring Security protects only a session the request names.
    private static RequestPostProcessor sessionCookie(MockHttpSession session) {
        return request -> {
            request.setRequestedSessionId(session.getId());
            request.setRequestedSessionIdValid(true);
            return request;
        };
    }

    private void passwordLogin(MockHttpSession session, String expectedRedirect) throws Exception {
        when(userAccounts.byEmail(EMAIL)).thenReturn(user);
        mvc.perform(post("/login").session(session)
                        .with(sessionCookie(session))
                        .param("username", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl(expectedRedirect));
    }

    @Test
    public void passwordLoginContinuesToTheAppRouteOpened() throws Exception {
        passwordLogin(openSignedOut("/app/party/7"), "http://localhost/app/party/7?continue");
    }

    @Test
    public void passwordLoginContinuesToTheClassicPageOpenedWithItsQuery() throws Exception {
        passwordLogin(openSignedOut("/ui/party?id=7"), "http://localhost/ui/party?id=7&continue");
    }

    @Test
    public void passwordLoginWithoutAPageOpenedLandsOnTheFrontPage() throws Exception {
        passwordLogin(new MockHttpSession(), "/app/");
    }

    @Test
    public void anApiCallFromASignedOutSessionIsNotReturnedTo() throws Exception {
        MockHttpSession session = (MockHttpSession) mvc.perform(get("/API/v2/profile").accept(MediaType.APPLICATION_JSON))
                .andReturn().getRequest().getSession();

        passwordLogin(session, "/app/");
    }

    @Test
    public void aJoinLinkOpenedAfterAnotherPageStillContinuesToTheJoin() throws Exception {
        MockHttpSession session = openSignedOut("/ui/party?id=7");
        mvc.perform(get("/join/AbCdEfGhIjKlMnOpQrSt-_").session(session))
                .andExpect(redirectedUrl("/ui/login?join"));

        passwordLogin(session, "/app/");
    }

    @Test
    public void registrationContinuesToThePageOpened() throws Exception {
        when(userAccounts.byEmail(EMAIL)).thenReturn(null, user);
        MockHttpSession session = openSignedOut("/app/party-admin/7");

        mvc.perform(post("/ui/addUser").session(session)
                        .param("name", "Vierailija")
                        .param("sex", "MALE")
                        .param("weight", "80")
                        .param("email", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("http://localhost/app/party-admin/7?continue"));
    }

    @Test
    public void googleSignInRelayedFromTheHubContinuesToThePageOpened() throws Exception {
        AuthRelayTokenService.HandoffClaims claims = new AuthRelayTokenService.HandoffClaims();
        claims.sub = "google-sub";
        claims.email = EMAIL;
        claims.name = "Vierailija";
        when(relayTokenService.verify(eq("handoff"), anyString())).thenReturn(claims);
        when(identityLinkingService.findOrCreateUser("google-sub", EMAIL, "Vierailija", null, null)).thenReturn(user);
        GoogleIdentityLinkingService realLinking = new GoogleIdentityLinkingService(userAccounts);
        doAnswer(call -> {
            realLinking.establishSession(call.getArgument(0), call.getArgument(1), call.<HttpServletRequest>getArgument(2));
            return null;
        }).when(identityLinkingService).establishSession(anyInt(), anyString(), any());
        MockHttpSession session = openSignedOut("/app/nearby");

        mvc.perform(get("/api/auth/relay/complete").session(session).param("token", "handoff"))
                .andExpect(redirectedUrl("http://localhost/app/nearby?continue"));
    }
}
