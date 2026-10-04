package drinkcounter.authentication;

import drinkcounter.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.Set;

/**
 * Handles OAuth2/OIDC social login by looking up or creating users via
 * {@link GoogleIdentityLinkingService}.
 */
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final GoogleIdentityLinkingService identityLinkingService;

    public CustomOAuth2UserService(GoogleIdentityLinkingService identityLinkingService) {
        this.identityLinkingService = identityLinkingService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Get OAuth2 user info from Google
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract attributes from Google profile
        String sub = oauth2User.getAttribute("sub"); // Google's unique user ID
        String email = oauth2User.getAttribute("email");
        String givenName = oauth2User.getAttribute("given_name");
        String familyName = oauth2User.getAttribute("family_name");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        if (sub == null) {
            throw new OAuth2AuthenticationException("User ID (sub) not found from OAuth2 provider");
        }

        User user = identityLinkingService.findOrCreateUser(sub, email, name, givenName, familyName);

        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));
        return new DrinkcounterOAuth2User(authorities, oauth2User.getAttributes(), new LoggedInUser(user.getId(), email));
    }
}
