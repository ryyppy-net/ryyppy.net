package drinkcounter.web.controllers.ui;

import jakarta.servlet.http.HttpSession;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.util.UriUtils;

/**
 * The address a party's join link points to. The join page is an AngularJS route, and the hash
 * of a URL never reaches the server, so the shared link is this server path instead.
 */
@Controller
public class JoinController {

    /** Holds a signed-out visitor's join token until DefaultController continues the join after sign-in. */
    public static final String PENDING_TOKEN_SESSION_ATTR = "partyJoin.token";

    @GetMapping("/join/{token}")
    public String join(@PathVariable String token, Principal principal, HttpSession session) {
        if (principal == null) {
            session.setAttribute(PENDING_TOKEN_SESSION_ATTR, token);
            return "redirect:/ui/login?join";
        }
        return joinPage(token);
    }

    public static String joinPage(String token) {
        return "redirect:/app/index.html#!/join/" + UriUtils.encodePathSegment(token, "UTF-8");
    }
}
