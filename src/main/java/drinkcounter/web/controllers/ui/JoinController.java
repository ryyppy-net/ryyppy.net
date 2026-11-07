package drinkcounter.web.controllers.ui;

import drinkcounter.authentication.SignInDestination;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriUtils;

/**
 * The address a party's join link points to. A signed-out visitor is sent to sign in first and
 * continues to the AngularJS join page afterwards.
 */
@Controller
public class JoinController {

    /** Holds a signed-out visitor's join token until DefaultController continues the join after sign-in. */
    public static final String PENDING_TOKEN_SESSION_ATTR = "partyJoin.token";

    private final SignInDestination signInDestination;

    public JoinController(SignInDestination signInDestination) {
        this.signInDestination = signInDestination;
    }

    @GetMapping("/join/{token}")
    public String join(@PathVariable String token, Principal principal, HttpSession session,
            HttpServletRequest request, HttpServletResponse response) {
        if (principal == null) {
            session.setAttribute(PENDING_TOKEN_SESSION_ATTR, token);
            // Signing in then lands on the app's front page, which continues the join.
            signInDestination.forget(request, response);
            return "redirect:/ui/login?join";
        }
        return joinPage(token);
    }

    public static String joinPage(String token) {
        return "redirect:/app/join/" + UriUtils.encodePathSegment(token, "UTF-8");
    }
}
