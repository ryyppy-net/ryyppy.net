package drinkcounter.mcp;

import java.net.URI;
import java.util.Optional;
import java.util.Set;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdMetadataDocumentRegisteredClientRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Component;

/** The authorization requests waiting for a user's consent, found by their {@code state}. */
@Component
public class McpConsentRequests {

    private static final OAuth2TokenType STATE = new OAuth2TokenType(OAuth2ParameterNames.STATE);

    private final OAuth2AuthorizationService authorizations;
    private final ClientIdMetadataDocumentRegisteredClientRepository registeredClients;

    public McpConsentRequests(OAuth2AuthorizationService authorizations,
            ClientIdMetadataDocumentRegisteredClientRepository registeredClients) {
        this.authorizations = authorizations;
        this.registeredClients = registeredClients;
    }

    /** The request with this state, if it is waiting for the consent of this user. */
    public Optional<McpConsentRequest> find(String state, String principalName) {
        OAuth2Authorization authorization = authorizations.findByToken(state, STATE);
        if (authorization == null || !authorization.getPrincipalName().equals(principalName)) {
            return Optional.empty();
        }
        OAuth2AuthorizationRequest authorizationRequest = authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        if (authorizationRequest == null) {
            return Optional.empty();
        }
        RegisteredClient client = registeredClients.findByClientId(authorizationRequest.getClientId());
        if (client == null) {
            return Optional.empty();
        }
        String redirectUri = authorizationRequest.getRedirectUri() != null
                ? authorizationRequest.getRedirectUri()
                : client.getRedirectUris().iterator().next();
        return Optional.of(new McpConsentRequest(client.getClientId(), client.getClientName(),
                URI.create(client.getClientId()).getHost(), URI.create(redirectUri).getHost(),
                state, authorizationRequest.getScopes()));
    }

    /**
     * What the consent page shows of a request. The client's name comes from its own metadata
     * document, so the page shows the hosts it is served from and redirects to beside it.
     */
    public record McpConsentRequest(String clientId, String clientName, String clientHost, String redirectHost,
            String state, Set<String> scopes) {

        /** Whether the access goes to a program on the user's own computer. */
        public boolean redirectsToLocalhost() {
            return "localhost".equalsIgnoreCase(redirectHost) || redirectHost.startsWith("127.") || "[::1]".equals(redirectHost);
        }
    }
}
