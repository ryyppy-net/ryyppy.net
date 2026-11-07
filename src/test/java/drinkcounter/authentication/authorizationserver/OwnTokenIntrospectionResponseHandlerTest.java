package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenIntrospection;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenIntrospectionAuthenticationToken;

public class OwnTokenIntrospectionResponseHandlerTest {

    private final OwnTokenIntrospectionResponseHandler handler = new OwnTokenIntrospectionResponseHandler();

    @Test
    public void appSeesItsOwnActiveToken() throws Exception {
        String body = respond("app", "app");

        assertTrue(body.contains("\"active\":true"), body);
        assertTrue(body.contains("ville@example.com"), body);
    }

    @Test
    public void anotherAppsTokenReadsAsInactive() throws Exception {
        String body = respond("other", "app");

        assertTrue(body.contains("\"active\":false"), body);
        assertTrue(!body.contains("ville@example.com"), body);
    }

    private String respond(String askingClientId, String tokenClientId) throws Exception {
        OAuth2ClientAuthenticationToken asking = new OAuth2ClientAuthenticationToken(publicClient(askingClientId),
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC, null);
        OAuth2TokenIntrospection claims = OAuth2TokenIntrospection.builder(true)
                .clientId(tokenClientId)
                .username("ville@example.com")
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response,
                new OAuth2TokenIntrospectionAuthenticationToken("token", asking, claims));
        return response.getContentAsString();
    }
}
