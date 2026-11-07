package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.REDIRECT_URI;
import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.UserAccounts;
import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import java.util.ArrayList;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebTest
public class AuthorizationServerWebTest {

    private static final String AUTHORIZATION_REQUEST = "/oauth2/authorize?response_type=code&client_id=app"
            + "&redirect_uri=" + REDIRECT_URI + "&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM"
            + "&code_challenge_method=S256&state=xyz";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegisteredClientRepository registeredClients;

    @Autowired
    private UserAccounts userAccounts;

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void authorizationStartedWhileSignedOutContinuesAfterSigningIn() throws Exception {
        when(registeredClients.findByClientId("app")).thenReturn(publicClient("app"));
        User user = new User();
        user.setId(42);
        user.setParties(new ArrayList<>());
        when(userAccounts.get(42)).thenReturn(user);
        MockHttpSession session = new MockHttpSession();

        mvc.perform(get(AUTHORIZATION_REQUEST).session(session).with(anonymous()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/ui/login?connect"));

        mvc.perform(get("/app/index.html").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost" + AUTHORIZATION_REQUEST));
        mvc.perform(get("/app/index.html").session(session))
                .andExpect(status().isOk());
    }

    @Test
    public void anyAppCanRegisterButOnlyForTheCodeFlowAndTheMcpScope() throws Exception {
        mvc.perform(post("/oauth2/register").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client_name": "Some app", "redirect_uris": ["https://app.example/callback"],
                                 "token_endpoint_auth_method": "none",
                                 "grant_types": ["client_credentials", "authorization_code"], "scope": "admin"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scope").value("mcp"))
                .andExpect(jsonPath("$.token_endpoint_auth_method").value("none"));

        ArgumentCaptor<RegisteredClient> saved = ArgumentCaptor.forClass(RegisteredClient.class);
        verify(registeredClients).save(saved.capture());
        assertEquals(Set.of(AuthorizationGrantType.AUTHORIZATION_CODE, AuthorizationGrantType.REFRESH_TOKEN),
                saved.getValue().getAuthorizationGrantTypes());
        assertEquals(Set.of("mcp"), saved.getValue().getScopes());
        assertEquals(false, saved.getValue().getTokenSettings().isReuseRefreshTokens());
    }
}
