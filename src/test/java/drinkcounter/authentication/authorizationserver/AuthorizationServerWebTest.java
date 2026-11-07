package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.REDIRECT_URI;
import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.UserAccounts;
import drinkcounter.model.User;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
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

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Test
    public void authorizationStartedWhileSignedOutContinuesAfterSigningIn() throws Exception {
        when(registeredClients.findByClientId("app")).thenReturn(publicClient("app"));
        User user = new User();
        user.setId(42);
        user.setEmail("ville@example.com");
        user.setPassword(passwordEncoder.encode("secret"));
        when(userAccounts.byEmail("ville@example.com")).thenReturn(user);

        MockHttpSession session = (MockHttpSession) mvc.perform(get(AUTHORIZATION_REQUEST))
                .andExpect(redirectedUrl("/ui/login?connect"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(post("/login").session(session)
                        .with(request -> {
                            request.setRequestedSessionId(session.getId());
                            request.setRequestedSessionIdValid(true);
                            return request;
                        })
                        .param("username", "ville@example.com")
                        .param("password", "secret"))
                .andExpect(redirectedUrl("http://localhost" + AUTHORIZATION_REQUEST + "&continue"));
    }

    @Test
    public void appsAreIdentifiedByClientMetadataDocumentsAndCannotRegister() throws Exception {
        mvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.client_id_metadata_document_supported").value(true))
                .andExpect(jsonPath("$.registration_endpoint").doesNotExist());

        mvc.perform(post("/oauth2/register").with(anonymous())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"client_name": "Some app", "redirect_uris": ["https://app.example/callback"]}
                                """))
                .andExpect(status().is4xxClientError());
        verify(registeredClients, never()).save(any());
    }
}
