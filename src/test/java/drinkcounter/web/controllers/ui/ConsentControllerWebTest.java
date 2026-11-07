package drinkcounter.web.controllers.ui;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.awaitingConsent;
import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static org.hamcrest.Matchers.contains;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.web.ControllerWebTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.test.web.servlet.MockMvc;

@ControllerWebTest
@WithDrinkcounterUser(userId = 42, email = "ville@example.com")
public class ConsentControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private RegisteredClientRepository registeredClients;

    @Autowired
    private OAuth2AuthorizationService authorizations;

    private final RegisteredClient client = publicClient("app");

    @BeforeEach
    public void setUp() {
        when(registeredClients.findByClientId("app")).thenReturn(client);
    }

    @Test
    public void showsTheAppAndWhereTheAnswerSendsTheUser() throws Exception {
        when(authorizations.findByToken(eq("pending"), any())).thenReturn(awaitingConsent(client, "pending", "ville@example.com", 42));

        mvc.perform(get("/ui/consent").param("client_id", "app").param("state", "pending").param("scope", "mcp"))
                .andExpect(status().isOk())
                .andExpect(view().name("consent"))
                .andExpect(model().attribute("clientName", "Test app"))
                .andExpect(model().attribute("redirectsTo", "http://127.0.0.1:33418"))
                .andExpect(model().attribute("email", "ville@example.com"))
                .andExpect(model().attribute("scopes", contains("mcp")));
    }

    @Test
    public void anotherUsersAuthorizationRequestIsNotShown() throws Exception {
        when(authorizations.findByToken(eq("pending"), any())).thenReturn(awaitingConsent(client, "pending", "someone@example.com", 7));

        mvc.perform(get("/ui/consent").param("client_id", "app").param("state", "pending"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist("clientName"));
    }

    @Test
    public void unknownRequestIsNotShown() throws Exception {
        mvc.perform(get("/ui/consent").param("client_id", "app").param("state", "unknown"))
                .andExpect(status().isBadRequest())
                .andExpect(model().attributeDoesNotExist("clientName"));
    }

    @Test
    public void requestOfAnotherAppIsNotShown() throws Exception {
        when(authorizations.findByToken(eq("pending"), any())).thenReturn(awaitingConsent(publicClient("other"), "pending", "ville@example.com", 42));

        mvc.perform(get("/ui/consent").param("client_id", "app").param("state", "pending"))
                .andExpect(status().isBadRequest());
    }
}
