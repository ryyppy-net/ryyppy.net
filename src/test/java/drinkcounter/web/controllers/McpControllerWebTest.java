package drinkcounter.web.controllers;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.withAccessToken;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.web.ControllerWebTest;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ControllerWebTest
@WithAnonymousUser
public class McpControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OAuth2AuthorizationService authorizations;

    @Test
    public void requestWithoutTokenIsToldWhereToGetOne() throws Exception {
        mvc.perform(post("/mcp"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate",
                        endsWith("resource_metadata=\"http://localhost/.well-known/oauth-protected-resource/mcp\"")));
    }

    @Test
    public void protectedResourceMetadataNamesThisServerAndTheMcpScope() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("http://localhost/mcp"))
                .andExpect(jsonPath("$.authorization_servers", contains("http://localhost")))
                .andExpect(jsonPath("$.scopes_supported", contains("mcp")));
    }

    @Test
    public void validAccessTokenReachesMcpWithoutStartingASession() throws Exception {
        OAuth2Authorization authorization = withAccessToken(publicClient("app"), "valid-token", Instant.now(), "ville@example.com", 42);
        when(authorizations.findByToken(eq("valid-token"), eq(OAuth2TokenType.ACCESS_TOKEN))).thenReturn(authorization);

        MvcResult result = mvc.perform(post("/mcp").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotImplemented())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    public void unknownAccessTokenIsUnauthorized() throws Exception {
        when(authorizations.findByToken(anyString(), eq(OAuth2TokenType.ACCESS_TOKEN))).thenReturn(null);

        mvc.perform(post("/mcp").header("Authorization", "Bearer unknown-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", endsWith("oauth-protected-resource/mcp\"")));
    }

    @Test
    public void expiredAccessTokenIsUnauthorized() throws Exception {
        OAuth2Authorization authorization = withAccessToken(publicClient("app"), "old-token",
                Instant.now().minus(Duration.ofHours(2)), "ville@example.com", 42);
        when(authorizations.findByToken(eq("old-token"), eq(OAuth2TokenType.ACCESS_TOKEN))).thenReturn(authorization);

        mvc.perform(post("/mcp").header("Authorization", "Bearer old-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void signedInBrowserSessionCannotUseMcp() throws Exception {
        mvc.perform(post("/mcp"))
                .andExpect(status().isForbidden());
    }
}
