package drinkcounter.authentication;

import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Completes the cross-environment Google sign-in relay: verifies the signed handoff token minted
 * by the hub environment (see GoogleOneTapController) and establishes a session here, on the
 * environment the user actually started on.
 */
@Controller
public class AuthRelayController {

    private static final Logger log = LoggerFactory.getLogger(AuthRelayController.class);

    private final AuthRelayTokenService tokenService;
    private final GoogleIdentityLinkingService identityLinkingService;

    public AuthRelayController(AuthRelayTokenService tokenService, GoogleIdentityLinkingService identityLinkingService) {
        this.tokenService = tokenService;
        this.identityLinkingService = identityLinkingService;
    }

    @GetMapping("/api/auth/relay/complete")
    public String complete(@RequestParam("token") String token, HttpServletRequest request) {
        String ownOrigin = Origins.of(request);

        AuthRelayTokenService.HandoffClaims claims;
        try {
            claims = tokenService.verify(token, ownOrigin);
        } catch (AuthRelayException e) {
            log.warn("Auth relay handoff rejected: {}", e.getMessage());
            return "redirect:/ui/login?error=relay_failed";
        }

        User user = identityLinkingService.findOrCreateUser(claims.sub, claims.email, claims.name, null, null);
        identityLinkingService.establishSession(user, claims.email, request);

        log.info("Auth relay: authentication completed for user: email={}, userId={}", claims.email, user.getId());

        return "redirect:/app/index.html";
    }
}
