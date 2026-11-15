package drinkcounter.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;

class PublicClientRefreshTokenTest {

    private static final String CLIENT_ID = "https://claude.ai/oauth/claude-code-client-metadata";

    private final PublicClientRefreshTokenAuthenticationConverter converter = new PublicClientRefreshTokenAuthenticationConverter();
    private final RegisteredClientRepository registeredClients = mock(RegisteredClientRepository.class);
    private final PublicClientRefreshTokenAuthenticationProvider provider =
            new PublicClientRefreshTokenAuthenticationProvider(registeredClients);
    private final PublicClientRefreshTokenGenerator generator = new PublicClientRefreshTokenGenerator();

    @Test
    void readsARefreshRequestThatHasOnlyAClientId() {
        OAuth2ClientAuthenticationToken token = (OAuth2ClientAuthenticationToken) converter.convert(refreshRequest());

        assertEquals(CLIENT_ID, token.getPrincipal());
        assertEquals(ClientAuthenticationMethod.NONE, token.getClientAuthenticationMethod());
        assertEquals("refresh_token", token.getAdditionalParameters().get("grant_type"));
        assertEquals("http://localhost:8080/mcp", token.getAdditionalParameters().get("resource"));
    }

    @Test
    void leavesRequestsWithClientCredentialsOrOtherGrantsToTheOtherConverters() {
        MockHttpServletRequest withSecret = refreshRequest();
        withSecret.addParameter("client_secret", "secret");
        MockHttpServletRequest withBasicAuth = refreshRequest();
        withBasicAuth.addHeader("Authorization", "Basic Y2xpZW50OnNlY3JldA==");
        MockHttpServletRequest codeGrant = refreshRequest();
        codeGrant.setParameter("grant_type", "authorization_code");

        assertNull(converter.convert(withSecret));
        assertNull(converter.convert(withBasicAuth));
        assertNull(converter.convert(codeGrant));
    }

    @Test
    void authenticatesAPublicClientAllowedTheRefreshGrant() {
        RegisteredClient client = client(ClientAuthenticationMethod.NONE, AuthorizationGrantType.REFRESH_TOKEN);
        when(registeredClients.findByClientId(CLIENT_ID)).thenReturn(client);

        OAuth2ClientAuthenticationToken authenticated =
                (OAuth2ClientAuthenticationToken) provider.authenticate(converter.convert(refreshRequest()));

        assertTrue(authenticated.isAuthenticated());
        assertSame(client, authenticated.getRegisteredClient());
    }

    @Test
    void rejectsAClientNotAllowedTheRefreshGrant() {
        when(registeredClients.findByClientId(CLIENT_ID))
                .thenReturn(client(ClientAuthenticationMethod.NONE, AuthorizationGrantType.AUTHORIZATION_CODE));

        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(converter.convert(refreshRequest())));
    }

    @Test
    void rejectsAConfidentialClientSendingNoCredentials() {
        when(registeredClients.findByClientId(CLIENT_ID))
                .thenReturn(client(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, AuthorizationGrantType.REFRESH_TOKEN));

        assertThrows(OAuth2AuthenticationException.class, () -> provider.authenticate(converter.convert(refreshRequest())));
    }

    @Test
    void issuesARefreshTokenToAPublicClientCompletingTheCodeGrant() {
        RegisteredClient client = client(ClientAuthenticationMethod.NONE, AuthorizationGrantType.REFRESH_TOKEN);

        OAuth2RefreshToken token = generator.generate(context(client, ClientAuthenticationMethod.NONE));

        assertNotNull(token);
        assertEquals(Duration.ofDays(365), Duration.between(token.getIssuedAt(), token.getExpiresAt()));
    }

    @Test
    void leavesConfidentialClientsToTheDefaultGenerator() {
        RegisteredClient client = client(ClientAuthenticationMethod.CLIENT_SECRET_BASIC, AuthorizationGrantType.REFRESH_TOKEN);

        assertNull(generator.generate(context(client, ClientAuthenticationMethod.CLIENT_SECRET_BASIC)));
    }

    private static MockHttpServletRequest refreshRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/oauth2/token");
        request.addParameter("grant_type", "refresh_token");
        request.addParameter("refresh_token", "a-refresh-token");
        request.addParameter("client_id", CLIENT_ID);
        request.addParameter("resource", "http://localhost:8080/mcp");
        return request;
    }

    private static RegisteredClient client(ClientAuthenticationMethod method, AuthorizationGrantType grant) {
        return RegisteredClient.withId(CLIENT_ID)
                .clientId(CLIENT_ID)
                .clientAuthenticationMethod(method)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(grant)
                .redirectUri("http://localhost/callback")
                .tokenSettings(TokenSettings.builder().refreshTokenTimeToLive(Duration.ofDays(365)).build())
                .build();
    }

    private static DefaultOAuth2TokenContext context(RegisteredClient client, ClientAuthenticationMethod method) {
        OAuth2ClientAuthenticationToken clientPrincipal = new OAuth2ClientAuthenticationToken(client, method, null);
        return DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrant(new OAuth2AuthorizationCodeAuthenticationToken("code", clientPrincipal,
                        "http://localhost/callback", Map.of()))
                .build();
    }
}
