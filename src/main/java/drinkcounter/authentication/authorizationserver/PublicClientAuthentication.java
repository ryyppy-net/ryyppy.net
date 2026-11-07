package drinkcounter.authentication.authorizationserver;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Identifies a public client by its {@code client_id} alone when it refreshes or revokes a token.
 * A public client has no secret, so the token it presents is its only credential; the refresh and
 * revocation endpoints then check that the token was issued to this client.
 */
public final class PublicClientAuthentication {

    private PublicClientAuthentication() {
    }

    public static class Converter implements AuthenticationConverter {

        private final RequestMatcher revocationRequest;

        public Converter(RequestMatcher revocationRequest) {
            this.revocationRequest = revocationRequest;
        }

        @Override
        public @Nullable Authentication convert(HttpServletRequest request) {
            boolean refresh = AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(request.getParameter(OAuth2ParameterNames.GRANT_TYPE));
            if (!(refresh || revocationRequest.matches(request))
                    || request.getHeader(HttpHeaders.AUTHORIZATION) != null
                    || request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null) {
                return null;
            }
            String[] clientIds = request.getParameterValues(OAuth2ParameterNames.CLIENT_ID);
            if (clientIds == null) {
                return null;
            }
            if (clientIds.length != 1 || clientIds[0].isBlank()) {
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_REQUEST);
            }
            Map<String, Object> parameters = new HashMap<>();
            request.getParameterMap().forEach((name, values) -> {
                if (!name.equals(OAuth2ParameterNames.CLIENT_ID)) {
                    parameters.put(name, values.length == 1 ? values[0] : values);
                }
            });
            return new OAuth2ClientAuthenticationToken(clientIds[0], ClientAuthenticationMethod.NONE, null, parameters);
        }
    }

    /** Leaves the authorization code grant to PKCE, which authenticates public clients there. */
    public static class Provider implements AuthenticationProvider {

        private final RegisteredClientRepository registeredClients;

        public Provider(RegisteredClientRepository registeredClients) {
            this.registeredClients = registeredClients;
        }

        @Override
        public @Nullable Authentication authenticate(Authentication authentication) {
            OAuth2ClientAuthenticationToken request = (OAuth2ClientAuthenticationToken) authentication;
            if (!ClientAuthenticationMethod.NONE.equals(request.getClientAuthenticationMethod())
                    || AuthorizationGrantType.AUTHORIZATION_CODE.getValue().equals(request.getAdditionalParameters().get(OAuth2ParameterNames.GRANT_TYPE))) {
                return null;
            }
            RegisteredClient client = registeredClients.findByClientId((String) request.getPrincipal());
            if (client == null || !client.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
                throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
            }
            return new OAuth2ClientAuthenticationToken(client, ClientAuthenticationMethod.NONE, null);
        }

        @Override
        public boolean supports(Class<?> authentication) {
            return OAuth2ClientAuthenticationToken.class.isAssignableFrom(authentication);
        }
    }
}
