package drinkcounter.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import drinkcounter.mcp.McpConsentRequests.McpConsentRequest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdMetadataDocumentRegisteredClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

class McpConsentRequestsTest {

    private static final String CLIENT_ID = "https://claude.ai/oauth/claude-code-client-metadata";

    private final OAuth2AuthorizationService authorizations = mock(OAuth2AuthorizationService.class);
    private final ClientIdMetadataDocumentRegisteredClientRepository registeredClients =
            mock(ClientIdMetadataDocumentRegisteredClientRepository.class);
    private final McpConsentRequests consentRequests = new McpConsentRequests(authorizations, registeredClients);

    private final RegisteredClient client = RegisteredClient.withId(CLIENT_ID)
            .clientId(CLIENT_ID)
            .clientName("Claude Code")
            .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("http://localhost/callback")
            .scope("mcp")
            .build();

    @Test
    void aWaitingRequestShowsTheClientAndWhereTheAccessGoes() {
        waiting("matti@example.com", "http://localhost:4567/callback");

        McpConsentRequest request = consentRequests.find("consent-state", "matti@example.com").orElseThrow();

        assertThat(request.clientId()).isEqualTo(CLIENT_ID);
        assertThat(request.clientName()).isEqualTo("Claude Code");
        assertThat(request.clientHost()).isEqualTo("claude.ai");
        assertThat(request.redirectHost()).isEqualTo("localhost");
        assertThat(request.redirectsToLocalhost()).isTrue();
        assertThat(request.scopes()).isEqualTo(Set.of("mcp"));
        assertThat(request.state()).isEqualTo("consent-state");
    }

    @Test
    void aRedirectToAWebsiteIsNotLocalhost() {
        waiting("matti@example.com", "https://claude.ai/api/mcp/auth_callback");

        McpConsentRequest request = consentRequests.find("consent-state", "matti@example.com").orElseThrow();

        assertThat(request.redirectHost()).isEqualTo("claude.ai");
        assertThat(request.redirectsToLocalhost()).isFalse();
    }

    @Test
    void anotherUsersRequestIsNotFound() {
        waiting("matti@example.com", "http://localhost:4567/callback");

        assertThat(consentRequests.find("consent-state", "liisa@example.com")).isEmpty();
    }

    @Test
    void anUnknownStateIsNotFound() {
        assertThat(consentRequests.find("unknown", "matti@example.com")).isEmpty();
    }

    private void waiting(String principalName, String redirectUri) {
        OAuth2AuthorizationRequest authorizationRequest = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://ryyppy.net/oauth2/authorize")
                .clientId(CLIENT_ID)
                .redirectUri(redirectUri)
                .scopes(Set.of("mcp"))
                .build();
        OAuth2Authorization authorization = OAuth2Authorization.withRegisteredClient(client)
                .principalName(principalName)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(OAuth2AuthorizationRequest.class.getName(), authorizationRequest)
                .attribute("state", "consent-state")
                .build();
        when(authorizations.findByToken(eq("consent-state"), any(OAuth2TokenType.class))).thenReturn(authorization);
        when(registeredClients.findByClientId(CLIENT_ID)).thenReturn(client);
    }
}
