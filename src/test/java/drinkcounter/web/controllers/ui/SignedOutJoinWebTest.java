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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;

/**
 * A visitor opens a join link signed out, signs in one of the app's ways, and is taken on to the
 * join page. The token is held in the session, so each sign-in must keep the session's attributes.
 * No user annotation: the session's own security context has to carry each request.
 */
@ControllerWebTest
public class SignedOutJoinWebTest {

    private static final String TOKEN = "AbCdEfGhIjKlMnOpQrSt-_";
    private static final String EMAIL = "joiner@example.com";
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
        user.setName("Liittyjä");
        user.setEmail(EMAIL);
        user.setPassword(passwordEncoder.encode(PASSWORD));
        user.setParties(new ArrayList<>());
        when(userAccounts.get(42)).thenReturn(user);
    }

    private MockHttpSession openJoinLinkSignedOut() throws Exception {
        return (MockHttpSession) mvc.perform(get("/join/" + TOKEN))
                .andExpect(redirectedUrl("/ui/login?join"))
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

    private void continuesToTheJoinPage(MockHttpSession session) throws Exception {
        mvc.perform(get("/app/index.html").session(session))
                .andExpect(redirectedUrl("/app/index.html#!/join/" + TOKEN))
                .andExpect(request().sessionAttributeDoesNotExist(JoinController.PENDING_TOKEN_SESSION_ATTR));
    }

    @Test
    public void passwordLoginContinuesToTheJoinPage() throws Exception {
        when(userAccounts.byEmail(EMAIL)).thenReturn(user);
        MockHttpSession session = openJoinLinkSignedOut();

        MockHttpSession signedIn = (MockHttpSession) mvc.perform(post("/login").session(session)
                        .with(sessionCookie(session))
                        .param("username", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/app/index.html"))
                .andReturn().getRequest().getSession(false);

        continuesToTheJoinPage(signedIn);
    }

    @Test
    public void registrationContinuesToTheJoinPage() throws Exception {
        when(userAccounts.byEmail(EMAIL)).thenReturn(null, user);
        MockHttpSession session = openJoinLinkSignedOut();

        mvc.perform(post("/ui/addUser").session(session)
                        .param("name", "Liittyjä")
                        .param("sex", "MALE")
                        .param("weight", "80")
                        .param("email", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(redirectedUrl("/app/index.html#!/"));

        continuesToTheJoinPage(session);
    }

    @Test
    public void googleSignInRelayedFromTheHubContinuesToTheJoinPage() throws Exception {
        AuthRelayTokenService.HandoffClaims claims = new AuthRelayTokenService.HandoffClaims();
        claims.sub = "google-sub";
        claims.email = EMAIL;
        claims.name = "Liittyjä";
        when(relayTokenService.verify(eq("handoff"), anyString())).thenReturn(claims);
        when(identityLinkingService.findOrCreateUser("google-sub", EMAIL, "Liittyjä", null, null)).thenReturn(user);
        GoogleIdentityLinkingService realLinking = new GoogleIdentityLinkingService(userAccounts);
        doAnswer(call -> {
            realLinking.establishSession(call.getArgument(0), call.getArgument(1), call.<HttpServletRequest>getArgument(2));
            return null;
        }).when(identityLinkingService).establishSession(anyInt(), anyString(), any());
        MockHttpSession session = openJoinLinkSignedOut();

        mvc.perform(get("/api/auth/relay/complete").session(session).param("token", "handoff"))
                .andExpect(redirectedUrl("/app/index.html"));

        continuesToTheJoinPage(session);
    }
}
