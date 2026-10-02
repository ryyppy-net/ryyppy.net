package drinkcounter.authentication.relay;

import drinkcounter.authentication.GoogleIdentityLinkingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Completes classical OAuth2 login on the hub. If this login was started via the relay (see
 * AuthRelayController#redirect / #start), mints a handoff token for the verified Google identity
 * and sends the browser back to the environment that started the sign-in. Otherwise signs the user
 * in on this environment with the same session One Tap establishes.
 */
public class RelayAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RelayAwareAuthenticationSuccessHandler.class);

    private final AuthRelayTokenService tokenService;
    private final GoogleIdentityLinkingService identityLinkingService;

    public RelayAwareAuthenticationSuccessHandler(AuthRelayTokenService tokenService,
            GoogleIdentityLinkingService identityLinkingService) {
        this.tokenService = tokenService;
        this.identityLinkingService = identityLinkingService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        HttpSession session = request.getSession(false);
        String returnTo = session != null
                ? (String) session.getAttribute(AuthRelayController.RETURN_TO_SESSION_ATTR)
                : null;
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");

        if (returnTo != null) {
            session.removeAttribute(AuthRelayController.RETURN_TO_SESSION_ATTR);

            String sub = oauth2User.getAttribute("sub");
            String name = oauth2User.getAttribute("name");

            String token = tokenService.mint(sub, email, name, returnTo);
            log.info("OAuth2 login: relaying verified Google identity to {}", returnTo);
            response.sendRedirect(returnTo + "/api/auth/relay/complete?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
            return;
        }

        identityLinkingService.establishSession(oauth2User.getAttribute("userId"), email, request);
        response.sendRedirect("/app/index.html");
    }
}
