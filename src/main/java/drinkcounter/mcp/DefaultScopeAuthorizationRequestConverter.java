package drinkcounter.mcp;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Reads an authorization request that names no scope as one for {@link McpSecurityConfiguration#MCP_SCOPE},
 * so that it, too, asks the user for consent.
 */
final class DefaultScopeAuthorizationRequestConverter implements AuthenticationConverter {

    private final AuthenticationConverter delegate;

    DefaultScopeAuthorizationRequestConverter(AuthenticationConverter delegate) {
        this.delegate = delegate;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        Authentication authentication = delegate.convert(request);
        if (authentication instanceof OAuth2AuthorizationCodeRequestAuthenticationToken authorizationRequest
                && authorizationRequest.getScopes().isEmpty()) {
            return new OAuth2AuthorizationCodeRequestAuthenticationToken(authorizationRequest.getAuthorizationUri(),
                    authorizationRequest.getClientId(), (Authentication) authorizationRequest.getPrincipal(),
                    authorizationRequest.getRedirectUri(), authorizationRequest.getState(),
                    Set.of(McpSecurityConfiguration.MCP_SCOPE), authorizationRequest.getAdditionalParameters());
        }
        return authentication;
    }
}
