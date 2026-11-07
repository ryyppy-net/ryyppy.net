package drinkcounter.authentication.authorizationserver;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Reads an authorization request that names no scope as asking for
 * {@value AuthorizationServerConfiguration#MCP_SCOPE}, the only scope there is. Consent can only
 * be given to a scope, so such a request could otherwise never be approved.
 */
public class DefaultScopeAuthorizationRequestConverter implements AuthenticationConverter {

    private final AuthenticationConverter delegate = new OAuth2AuthorizationCodeRequestAuthenticationConverter();

    @Override
    public @Nullable Authentication convert(HttpServletRequest request) {
        Authentication converted = delegate.convert(request);
        if (!(converted instanceof OAuth2AuthorizationCodeRequestAuthenticationToken authorizationRequest)
                || !authorizationRequest.getScopes().isEmpty()
                || authorizationRequest.getAdditionalParameters().containsKey(OAuth2ParameterNames.REQUEST_URI)) {
            return converted;
        }
        return new OAuth2AuthorizationCodeRequestAuthenticationToken(authorizationRequest.getAuthorizationUri(),
                authorizationRequest.getClientId(), (Authentication) authorizationRequest.getPrincipal(),
                authorizationRequest.getRedirectUri(), authorizationRequest.getState(),
                Set.of(AuthorizationServerConfiguration.MCP_SCOPE), authorizationRequest.getAdditionalParameters());
    }
}
