package drinkcounter.authentication.authorizationserver;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.StringJoiner;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

public class DefaultScopeAuthorizationRequestConverterTest {

    private static final String[] AUTHORIZATION_REQUEST = {
            "response_type", "code",
            "client_id", "app",
            "redirect_uri", AuthorizationFixtures.REDIRECT_URI,
            "state", "xyz",
            "code_challenge", "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            "code_challenge_method", "S256"};

    private final DefaultScopeAuthorizationRequestConverter converter = new DefaultScopeAuthorizationRequestConverter();

    @BeforeEach
    public void setUp() {
        AuthorizationServerContextHolder.setContext(new AuthorizationServerContext() {
            @Override
            public String getIssuer() {
                return "http://localhost";
            }

            @Override
            public AuthorizationServerSettings getAuthorizationServerSettings() {
                return AuthorizationServerSettings.builder().build();
            }
        });
    }

    @AfterEach
    public void tearDown() {
        AuthorizationServerContextHolder.resetContext();
    }

    @Test
    public void requestNamingNoScopeAsksForMcp() {
        OAuth2AuthorizationCodeRequestAuthenticationToken converted = convert(get(AUTHORIZATION_REQUEST));

        assertEquals(Set.of("mcp"), converted.getScopes());
        assertEquals("app", converted.getClientId());
        assertEquals("xyz", converted.getState());
        assertEquals("S256", converted.getAdditionalParameters().get("code_challenge_method"));
    }

    @Test
    public void requestedScopesAreKept() {
        MockHttpServletRequest request = get(AUTHORIZATION_REQUEST);
        addParameter(request, "scope", "other");

        assertEquals(Set.of("other"), convert(request).getScopes());
    }

    private OAuth2AuthorizationCodeRequestAuthenticationToken convert(MockHttpServletRequest request) {
        return (OAuth2AuthorizationCodeRequestAuthenticationToken) converter.convert(request);
    }

    private static MockHttpServletRequest get(String... parameters) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorize");
        for (int i = 0; i < parameters.length; i += 2) {
            addParameter(request, parameters[i], parameters[i + 1]);
        }
        return request;
    }

    /** The authorization endpoint reads a GET request's parameters from its query string. */
    private static void addParameter(MockHttpServletRequest request, String name, String value) {
        request.addParameter(name, value);
        StringJoiner query = new StringJoiner("&");
        if (request.getQueryString() != null) {
            query.add(request.getQueryString());
        }
        request.setQueryString(query.add(name + "=" + URLEncoder.encode(value, StandardCharsets.UTF_8)).toString());
    }
}
