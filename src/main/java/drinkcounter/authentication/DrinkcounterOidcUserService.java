package drinkcounter.authentication;

import drinkcounter.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collections;
import java.util.Set;

/**
 * Handles Google's OpenID Connect login: looks up or creates the user via
 * {@link GoogleIdentityLinkingService} and returns a {@link DrinkcounterOidcUser}.
 */
public class DrinkcounterOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate;
    private final GoogleIdentityLinkingService identityLinkingService;

    public DrinkcounterOidcUserService(OidcUserService delegate, GoogleIdentityLinkingService identityLinkingService) {
        this.delegate = delegate;
        this.identityLinkingService = identityLinkingService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        String email = oidcUser.getEmail();
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        User user = identityLinkingService.findOrCreateUser(oidcUser.getSubject(), email,
                oidcUser.getFullName(), oidcUser.getGivenName(), oidcUser.getFamilyName());

        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
        return new DrinkcounterOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo(),
                new LoggedInUser(user.getId(), email));
    }
}
