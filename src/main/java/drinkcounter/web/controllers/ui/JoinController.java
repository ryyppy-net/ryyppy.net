package drinkcounter.web.controllers.ui;

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

    @GetMapping("/join/{token}")
    public String join(@PathVariable String token, Principal principal) {
        if (principal == null) {
            return "redirect:/ui/login";
        }
        return "redirect:/app/index.html#!/join/" + UriUtils.encodePathSegment(token, "UTF-8");
    }
}
