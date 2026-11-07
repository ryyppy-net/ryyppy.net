package drinkcounter.web.controllers;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import drinkcounter.authentication.WithDrinkcounterUser;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@ControllerWebTest
@WithAnonymousUser
public class McpControllerWebTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtDecoder jwtDecoder;

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
    public void accessTokenWithTheMcpScopeReachesMcpWithoutStartingASession() throws Exception {
        when(jwtDecoder.decode("valid-token")).thenReturn(accessToken("valid-token", "mcp"));

        MvcResult result = mvc.perform(post("/mcp").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotImplemented())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    public void accessTokenWithoutTheMcpScopeIsForbidden() throws Exception {
        when(jwtDecoder.decode("other-token")).thenReturn(accessToken("other-token", "other"));

        mvc.perform(post("/mcp").header("Authorization", "Bearer other-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    public void invalidAccessTokenIsUnauthorized() throws Exception {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("bad signature"));

        mvc.perform(post("/mcp").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", endsWith("oauth-protected-resource/mcp\"")));
    }

    @Test
    @WithDrinkcounterUser(userId = 42)
    public void signedInBrowserSessionCannotUseMcp() throws Exception {
        mvc.perform(post("/mcp"))
                .andExpect(status().isForbidden());
    }

    private static Jwt accessToken(String value, String scope) {
        return Jwt.withTokenValue(value)
                .header("alg", "RS256")
                .subject("42")
                .claim("scope", List.of(scope))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
