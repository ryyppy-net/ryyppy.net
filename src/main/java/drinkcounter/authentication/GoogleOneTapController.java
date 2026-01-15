package drinkcounter.authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import drinkcounter.UserService;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Collections;
import java.util.List;

/**
 * Handles Google One Tap sign-in by verifying the credential token
 * and establishing a Spring Security session.
 */
@Controller
public class GoogleOneTapController {

    private static final Logger log = LoggerFactory.getLogger(GoogleOneTapController.class);

    private final UserService userService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public GoogleOneTapController(UserService userService, ClientRegistrationRepository clientRegistrationRepository) {
        this.userService = userService;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    @PostMapping("/api/auth/google/one-tap")
    public String handleOneTapCallback(
            @RequestParam("credential") String credential,
            HttpServletRequest request) {

        if (credential == null || credential.isEmpty()) {
            log.warn("One Tap callback received without credential");
            return "redirect:/ui/login?error=missing_credential";
        }

        // Get Google client ID from configuration
        ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");
        if (googleRegistration == null) {
            log.error("Google OAuth2 registration not found");
            return "redirect:/ui/login?error=google_not_configured";
        }

        String clientId = googleRegistration.getClientId();

        // Verify the token
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();

        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(credential);
        } catch (Exception e) {
            log.error("Failed to verify Google ID token", e);
            return "redirect:/ui/login?error=token_verification_failed";
        }

        if (idToken == null) {
            log.warn("Invalid Google ID token received");
            return "redirect:/ui/login?error=invalid_token";
        }

        // Extract claims from the token
        GoogleIdToken.Payload payload = idToken.getPayload();
        String sub = payload.getSubject(); // Google's unique user ID
        String email = payload.getEmail();
        String name = (String) payload.get("name");
        String givenName = (String) payload.get("given_name");
        String familyName = (String) payload.get("family_name");

        if (email == null) {
            log.warn("Google token missing email claim");
            return "redirect:/ui/login?error=email_not_provided";
        }

        // Look up or create user (duplicated from CustomOAuth2UserService)
        User user = userService.getUserByOpenId(sub);

        if (user == null) {
            // Fallback: try to find user by email (for account linking)
            user = userService.getUserByEmail(email);

            if (user != null) {
                // Existing user found by email - link Google account automatically
                user.setOpenId(sub);
                userService.updateUser(user);
                log.info("One Tap: Linked Google account to existing user: email={}, userId={}, googleId={}", email, user.getId(), sub);
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
                log.info("One Tap: Created new user: email={}, name={}, googleId={}", email, user.getName(), sub);
            }
        } else {
            log.info("One Tap: Existing user logged in: email={}, userId={}, googleId={}", email, user.getId(), sub);
        }

        // Create authentication and set in security context
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

        // Save security context to session
        HttpSession session = request.getSession(true);
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        log.info("One Tap: Authentication successful for user: email={}, userId={}", email, user.getId());

        return "redirect:/app/index.html";
    }
}
