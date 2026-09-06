package drinkcounter.authentication.relay;

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
 * and sends the browser back to the environment that started the sign-in. Otherwise behaves like
 * an ordinary successful login on this environment.
 */
public class RelayAwareAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(RelayAwareAuthenticationSuccessHandler.class);

    private final AuthRelayTokenService tokenService;

    public RelayAwareAuthenticationSuccessHandler(AuthRelayTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException {
        HttpSession session = request.getSession(false);
        String returnTo = session != null
                ? (String) session.getAttribute(AuthRelayController.RETURN_TO_SESSION_ATTR)
                : null;

        if (returnTo != null) {
            session.removeAttribute(AuthRelayController.RETURN_TO_SESSION_ATTR);

            OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
            String sub = oauth2User.getAttribute("sub");
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");

            String token = tokenService.mint(sub, email, name, returnTo);
            log.info("OAuth2 login: relaying verified Google identity to {}", returnTo);
            response.sendRedirect(returnTo + "/api/auth/relay/complete?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8));
            return;
        }

        response.sendRedirect("/app/index.html");
    }
}
