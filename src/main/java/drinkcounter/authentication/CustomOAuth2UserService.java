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
 * Handles OAuth2/OIDC social login by looking up or creating users.
 *
 * User lookup: First by openId (Google's sub claim), then by email.
 * If found by email, stores the Google ID in openId for future lookups.
 * If not found, creates a new user with defaults (70kg, MALE, OPENID).
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

        // Look up existing user by Google ID (openId)
        User user = userService.getUserByOpenId(sub);

        if (user == null) {
            // Fallback: try to find user by email (for account linking)
            user = userService.getUserByEmail(email);

            if (user != null) {
                // Existing user found by email - link Google account automatically
                user.setOpenId(sub);
                userService.updateUser(user);
                log.info("Linked Google account to existing user: email={}, userId={}, googleId={}", email, user.getId(), sub);
            } else {
                // Create new user with default values
                user = new User();
                user.setEmail(email);
                user.setOpenId(sub);

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
                user.setAuthMethod(User.AuthMethod.OPENID);
                user.setGuest(false);

                user = userService.addUser(user);
                log.info("Created new user via Google OAuth2: email={}, name={}, googleId={}", email, user.getName(), sub);
            }
        } else {
            log.info("Existing user logged in via Google OAuth2: email={}, userId={}, googleId={}", email, user.getId(), sub);
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
