package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

public class PublicClientAuthenticationTest {

    private final PublicClientAuthentication.Converter converter = new PublicClientAuthentication.Converter(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, "/oauth2/revoke"));
    private final RegisteredClientRepository registeredClients = mock(RegisteredClientRepository.class);
    private final PublicClientAuthentication.Provider provider = new PublicClientAuthentication.Provider(registeredClients);

    @Test
    public void refreshWithOnlyAClientIdIsReadAsAPublicClient() {
        OAuth2ClientAuthenticationToken token = (OAuth2ClientAuthenticationToken) converter.convert(
                request("/oauth2/token", "grant_type", "refresh_token", "client_id", "app", "refresh_token", "r"));

        assertNotNull(token);
        assertEquals("app", token.getPrincipal());
        assertEquals(ClientAuthenticationMethod.NONE, token.getClientAuthenticationMethod());
    }

    @Test
    public void revocationWithOnlyAClientIdIsReadAsAPublicClient() {
        assertNotNull(converter.convert(request("/oauth2/revoke", "client_id", "app", "token", "t")));
    }

    @Test
    public void requestsWithASecretOrForAnotherGrantAreLeftToTheOtherConverters() {
        assertNull(converter.convert(request("/oauth2/token", "grant_type", "refresh_token", "client_id", "app", "client_secret", "s")));
        assertNull(converter.convert(request("/oauth2/token", "grant_type", "authorization_code", "client_id", "app", "code", "c")));
        assertNull(converter.convert(request("/oauth2/introspect", "client_id", "app", "token", "t")));
    }

    @Test
    public void registeredPublicClientIsAuthenticated() {
        when(registeredClients.findByClientId("app")).thenReturn(publicClient("app"));

        Authentication authenticated = provider.authenticate(clientIdOnly("app", Map.of("grant_type", "refresh_token")));

        assertTrue(authenticated.isAuthenticated());
        assertEquals("app", ((OAuth2ClientAuthenticationToken) authenticated).getRegisteredClient().getClientId());
    }

    @Test
    public void clientWithASecretCannotAuthenticateWithoutIt() {
        RegisteredClient confidential = RegisteredClient.from(publicClient("app"))
                .clientAuthenticationMethods(methods -> {
                    methods.clear();
                    methods.add(ClientAuthenticationMethod.CLIENT_SECRET_BASIC);
                })
                .clientSecret("{noop}secret")
                .build();
        when(registeredClients.findByClientId("app")).thenReturn(confidential);

        assertThrows(OAuth2AuthenticationException.class,
                () -> provider.authenticate(clientIdOnly("app", Map.of("grant_type", "refresh_token"))));
    }

    @Test
    public void unknownClientIsRejected() {
        assertThrows(OAuth2AuthenticationException.class,
                () -> provider.authenticate(clientIdOnly("nobody", Map.of("token", "t"))));
    }

    @Test
    public void codeExchangeIsLeftToProofKeyAuthentication() {
        assertNull(provider.authenticate(clientIdOnly("app", Map.of("grant_type", "authorization_code"))));
    }

    private static OAuth2ClientAuthenticationToken clientIdOnly(String clientId, Map<String, Object> parameters) {
        return new OAuth2ClientAuthenticationToken(clientId, ClientAuthenticationMethod.NONE, null, parameters);
    }

    private static MockHttpServletRequest request(String path, String... parameters) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        for (int i = 0; i < parameters.length; i += 2) {
            request.addParameter(parameters[i], parameters[i + 1]);
        }
        return request;
    }
}
