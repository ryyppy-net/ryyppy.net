package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Custom OAuth2UserService that handles Google login.
 * Looks up existing users by email or creates new users with default values.
 * Note: This is instantiated as a bean in GoogleAuthConfiguration when google.auth.enabled=true
 */
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private static final Logger log = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    private final UserService userService;

    public CustomOAuth2UserService(UserService userService) {
        this.userService = userService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // Get OAuth2 user info from Google
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract email from Google profile
        String email = oauth2User.getAttribute("email");
        String givenName = oauth2User.getAttribute("given_name");
        String familyName = oauth2User.getAttribute("family_name");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Look up existing user by email
        User user = userService.getUserByEmail(email);

        if (user == null) {
            // Create new user with default values
            user = new User();
            user.setEmail(email);

            // Set name from Google profile
            if (name != null && !name.isEmpty()) {
                user.setName(name);
            } else if (givenName != null) {
                user.setName(givenName + (familyName != null ? " " + familyName : ""));
            } else {
                user.setName(email); // Fallback to email if no name provided
            }

            // Set defaults
            user.setWeight(70f);
            user.setSex(User.Sex.MALE);
            user.setAuthMethod(User.AuthMethod.PASSWORD);
            user.setGuest(false);

            user = userService.addUser(user);
            log.info("Created new user via Google OAuth2: email={}, name={}", email, user.getName());
        } else {
            log.info("Existing user logged in via Google OAuth2: email={}, userId={}", email, user.getId());
        }

        // Create authorities
        Set<GrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        // Add userId to attributes so we can access it later
        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put("userId", user.getId());

        // Return OAuth2User with user details
        return new DefaultOAuth2User(authorities, attributes, "email");
    }
}
