package drinkcounter.authentication.authorizationserver;

import drinkcounter.authentication.DrinkcounterUserDetails;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

/**
 * Signs a request in as the user who authorized its access token, with the same
 * {@link DrinkcounterUserDetails} principal a browser session has, so the access rules apply
 * unchanged. Access tokens are opaque and live in the authorization service's table.
 */
public class AccessTokenAuthenticationManager implements AuthenticationManager {

    private final OAuth2AuthorizationService authorizations;

    public AccessTokenAuthenticationManager(OAuth2AuthorizationService authorizations) {
        this.authorizations = authorizations;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String tokenValue = ((BearerTokenAuthenticationToken) authentication).getToken();
        OAuth2Authorization authorization = authorizations.findByToken(tokenValue, OAuth2TokenType.ACCESS_TOKEN);
        OAuth2Authorization.Token<OAuth2AccessToken> accessToken = authorization == null ? null : authorization.getAccessToken();
        if (accessToken == null || !accessToken.isActive()
                || !(authorization.getAttribute(Principal.class.getName()) instanceof Authentication signedIn)
                || !(signedIn.getPrincipal() instanceof DrinkcounterUserDetails user)) {
            throw new InvalidBearerTokenException("The access token is unknown, expired or revoked");
        }
        List<GrantedAuthority> authorities = new ArrayList<>(user.getAuthorities());
        accessToken.getToken().getScopes().forEach(scope -> authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope)));
        return UsernamePasswordAuthenticationToken.authenticated(user, null, authorities);
    }
}
