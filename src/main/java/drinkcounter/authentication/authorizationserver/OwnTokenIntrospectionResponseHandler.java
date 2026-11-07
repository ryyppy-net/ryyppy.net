package drinkcounter.authentication.authorizationserver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenIntrospection;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2TokenIntrospectionAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.http.converter.OAuth2TokenIntrospectionHttpMessageConverter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/**
 * Answers a token introspection request as inactive unless the token was issued to the app
 * asking. Any URL can be an app's client id, and a token's claims name the user who authorized it.
 */
public class OwnTokenIntrospectionResponseHandler implements AuthenticationSuccessHandler {

    private final OAuth2TokenIntrospectionHttpMessageConverter converter = new OAuth2TokenIntrospectionHttpMessageConverter();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        OAuth2TokenIntrospectionAuthenticationToken introspection = (OAuth2TokenIntrospectionAuthenticationToken) authentication;
        OAuth2TokenIntrospection claims = introspection.getTokenClaims();
        OAuth2ClientAuthenticationToken asking = (OAuth2ClientAuthenticationToken) introspection.getPrincipal();
        String askingClientId = asking.getRegisteredClient() == null ? null : asking.getRegisteredClient().getClientId();
        if (claims.isActive() && !Objects.equals(claims.getClientId(), askingClientId)) {
            claims = OAuth2TokenIntrospection.builder().build();
        }
        converter.write(claims, null, new ServletServerHttpResponse(response));
    }
}
