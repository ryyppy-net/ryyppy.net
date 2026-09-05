package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Shared by every Google sign-in path (direct One Tap, the relay handoff, and OAuth2 login) so
 * account lookup/creation and session establishment behave identically no matter which one
 * authenticated the user.
 */
@Service
public class GoogleIdentityLinkingService {

    private static final Logger log = LoggerFactory.getLogger(GoogleIdentityLinkingService.class);

    private final UserService userService;

    public GoogleIdentityLinkingService(UserService userService) {
        this.userService = userService;
    }

    /**
     * Finds the user matching a verified Google identity, linking or creating the account as
     * needed. googleSub is Google's "sub" claim, stored as User.openId.
     */
    public User findOrCreateUser(String googleSub, String email, String name, String givenName, String familyName) {
        User user = userService.getUserByOpenId(googleSub);
        if (user != null) {
            log.info("Existing user logged in via Google: email={}, userId={}, googleId={}", email, user.getId(), googleSub);
            return user;
        }

        user = userService.getUserByEmail(email);
        if (user != null) {
            user.setOpenId(googleSub);
            userService.updateUser(user);
            log.info("Linked Google account to existing user: email={}, userId={}, googleId={}", email, user.getId(), googleSub);
            return user;
        }

        user = new User();
        user.setEmail(email);
        user.setOpenId(googleSub);

        if (name != null && !name.isEmpty()) {
            user.setName(name);
        } else if (givenName != null) {
            user.setName(givenName + (familyName != null ? " " + familyName : ""));
        } else {
            user.setName(email);
        }

        user.setWeight(70f);
        user.setSex(User.Sex.MALE);
        user.setAuthMethod(User.AuthMethod.OPENID);
        user.setGuest(false);

        user = userService.addUser(user);
        log.info("Created new user via Google: email={}, name={}, googleId={}", email, user.getName(), googleSub);
        return user;
    }

    /** Sets the Spring Security context for this user and saves it into the HTTP session. */
    public void establishSession(User user, String email, HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));

        DrinkcounterUserDetails userDetails = new DrinkcounterUserDetails(
                email,
                "", // No password for OAuth users
                true, true, true, true,
                authorities,
                user.getId()
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );
    }
}
