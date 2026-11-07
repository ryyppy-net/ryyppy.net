package drinkcounter.authentication;

import drinkcounter.web.controllers.DefaultController;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

/**
 * Where the sign-ins that bypass Spring Security's login filters (One Tap, the auth relay,
 * registration) continue: the page Spring Security saved when it sent the visitor to sign in.
 */
@Component
public class SignInDestination {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    public String redirect(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) {
            return DefaultController.REDIRECT_TO_FRONTPAGE;
        }
        requestCache.removeRequest(request, response);
        return "redirect:" + saved.getRedirectUrl();
    }

    public void forget(HttpServletRequest request, HttpServletResponse response) {
        requestCache.removeRequest(request, response);
    }
}
