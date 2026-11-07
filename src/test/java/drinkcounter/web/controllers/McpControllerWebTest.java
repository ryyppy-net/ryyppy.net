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

import drinkcounter.authentication.authorizationserver.AuthorizationFixtures;
import drinkcounter.web.ControllerWebTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
    public void protectedResourceMetadataNamesThisServer() throws Exception {
        mvc.perform(get("/.well-known/oauth-protected-resource/mcp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resource").value("http://localhost/mcp"))
                .andExpect(jsonPath("$.authorization_servers", contains("http://localhost")));
    }

    @Test
    public void accessTokenReachesMcpWithoutStartingASession() throws Exception {
        when(jwtDecoder.decode("valid-token")).thenReturn(accessToken("valid-token"));

        MvcResult result = mvc.perform(post("/mcp").header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotImplemented())
                .andReturn();

        assertNull(result.getRequest().getSession(false));
    }

    @Test
    public void invalidAccessTokenIsUnauthorized() throws Exception {
        when(jwtDecoder.decode("invalid-token")).thenThrow(new BadJwtException("bad signature"));

        mvc.perform(post("/mcp").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", endsWith("oauth-protected-resource/mcp\"")));
    }

    @Test
    public void signedInBrowserSessionCannotUseMcp() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                new SecurityContextImpl(AuthorizationFixtures.signedIn("ville@example.com", 42)));

        mvc.perform(post("/mcp").session(session))
                .andExpect(status().isUnauthorized());
    }

    private static Jwt accessToken(String value) {
        return Jwt.withTokenValue(value)
                .header("alg", "RS256")
                .subject("42")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
