package drinkcounter.authentication.relay;

import drinkcounter.authentication.GoogleIdentityLinkingService;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Endpoints for the cross-environment Google sign-in relay.
 *
 * Google Identity Services (One Tap / the styled sign-in button) checks the *page's own origin*
 * against Google's Authorized JavaScript origins before it lets sign-in proceed at all - a check
 * that happens entirely on Google's side and can't be relayed, so it always fails with
 * origin_mismatch on Railway's ephemeral, unregistered PR domains. The classical OAuth2
 * authorization-code flow only checks the *redirect URI*, so instead of initiating it locally,
 * a non-hub environment bounces the user through the hub (the one domain actually registered with
 * Google):
 *
 *  1. {@link #redirect} (this environment) signs its own origin and sends the browser to the
 *     hub's {@link #start}.
 *  2. {@link #start} (the hub) verifies that signature, stashes the origin in its session, and
 *     kicks off Spring Security's normal /oauth2/authorization/google flow.
 *  3. On success, RelayAwareAuthenticationSuccessHandler mints a handoff token and sends the
 *     browser back to {@link #complete}, on the environment that started the sign-in.
 */
@Controller
public class AuthRelayController {

    private static final Logger log = LoggerFactory.getLogger(AuthRelayController.class);

    static final String RETURN_TO_SESSION_ATTR = "authRelay.returnTo";

    private final AuthRelayTokenService tokenService;
    private final GoogleIdentityLinkingService identityLinkingService;

    public AuthRelayController(AuthRelayTokenService tokenService, GoogleIdentityLinkingService identityLinkingService) {
        this.tokenService = tokenService;
        this.identityLinkingService = identityLinkingService;
    }

    @GetMapping("/api/auth/relay/redirect")
    public String redirect(HttpServletRequest request) {
        String hubUrl = System.getenv("GOOGLE_AUTH_HUB_URL");
        if (!tokenService.isEnabled() || hubUrl == null || hubUrl.isBlank()) {
            log.error("Google sign-in relay was requested but GOOGLE_AUTH_HUB_URL/AUTH_RELAY_SECRET are not configured");
            return "redirect:/ui/login?error=relay_not_configured";
        }

        String ownOrigin = Origins.of(request);
        String signature = tokenService.signOrigin(ownOrigin);

        log.info("Auth relay: {} starting sign-in via hub", ownOrigin);

        String target = hubUrl.replaceAll("/+$", "") + "/api/auth/relay/start"
                + "?return_to=" + URLEncoder.encode(ownOrigin, StandardCharsets.UTF_8)
                + "&sig=" + URLEncoder.encode(signature, StandardCharsets.UTF_8);
        return "redirect:" + target;
    }

    @GetMapping("/api/auth/relay/start")
    public String start(
            @RequestParam("return_to") String returnTo,
            @RequestParam("sig") String sig,
            HttpServletRequest request) {

        if (!tokenService.verifyOriginSignature(returnTo, sig)) {
            log.warn("Auth relay start rejected: invalid signature for return_to={}", returnTo);
            return "redirect:/ui/login?error=invalid_relay_request";
        }

        log.info("Auth relay: hub received valid start request for {}", returnTo);

        request.getSession(true).setAttribute(RETURN_TO_SESSION_ATTR, returnTo);
        return "redirect:/oauth2/authorization/google";
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
