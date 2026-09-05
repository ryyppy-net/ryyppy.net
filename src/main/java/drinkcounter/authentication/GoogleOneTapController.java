package drinkcounter.authentication;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Handles Google One Tap sign-in by verifying the credential token and either establishing a
 * Spring Security session directly, or - when the sign-in was started on an environment other
 * than the hub Google is actually registered for (see login.jsp's oneTapLoginUri) - relaying the
 * verified identity back to that environment. See AuthRelayController and AuthRelayTokenService.
 */
@Controller
public class GoogleOneTapController {

    private static final Logger log = LoggerFactory.getLogger(GoogleOneTapController.class);

    private final GoogleIdentityLinkingService identityLinkingService;
    private final AuthRelayTokenService relayTokenService;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public GoogleOneTapController(
            GoogleIdentityLinkingService identityLinkingService,
            AuthRelayTokenService relayTokenService,
            ClientRegistrationRepository clientRegistrationRepository) {
        this.identityLinkingService = identityLinkingService;
        this.relayTokenService = relayTokenService;
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

        // login.jsp always points One Tap's login_uri at the hub, so on any other environment
        // this POST arrives here with a Referer pointing back at where the sign-in started.
        String ownOrigin = Origins.of(request);
        String refererOrigin = Origins.ofReferer(request);
        boolean crossEnvironment = refererOrigin != null && !refererOrigin.equalsIgnoreCase(ownOrigin);

        if (crossEnvironment) {
            if (!relayTokenService.isEnabled()) {
                log.error("One Tap credential POSTed cross-origin (referer={}) but AUTH_RELAY_SECRET is not configured", refererOrigin);
                return "redirect:/ui/login?error=relay_not_configured";
            }

            String token = relayTokenService.mint(sub, email, name, refererOrigin);
            log.info("One Tap: relaying verified Google identity to {}", refererOrigin);
            return "redirect:" + refererOrigin + "/api/auth/relay/complete?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8);
        }

        User user = identityLinkingService.findOrCreateUser(sub, email, name, givenName, familyName);
        identityLinkingService.establishSession(user, email, request);

        log.info("One Tap: authentication successful for user: email={}, userId={}", email, user.getId());

        return "redirect:/app/index.html";
    }
}
