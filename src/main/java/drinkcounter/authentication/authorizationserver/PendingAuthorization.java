package drinkcounter.authentication.authorizationserver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * Holds an app's authorization request while the user signs in, so that signing in continues to
 * the consent page. Kept apart from other saved requests: everywhere else, signing in always
 * lands on the dashboard.
 */
public final class PendingAuthorization {

    static final RequestMatcher AUTHORIZATION_REQUEST = PathPatternRequestMatcher.withDefaults()
            .matcher(HttpMethod.GET, AuthorizationServerSettings.builder().build().getAuthorizationEndpoint());

    private static final HttpSessionRequestCache CACHE = new HttpSessionRequestCache();

    static {
        CACHE.setSessionAttrName("oauth2Authorization.pendingRequest");
        CACHE.setMatchingRequestParameterName(null);
        CACHE.setRequestMatcher(AUTHORIZATION_REQUEST);
    }

    private PendingAuthorization() {
    }

    static RequestCache requestCache() {
        return CACHE;
    }

    /** The pending authorization request's URL, removed from the session; null if there is none. */
    public static @Nullable String take(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = CACHE.getRequest(request, response);
        if (saved == null) {
            return null;
        }
        CACHE.removeRequest(request, response);
        return saved.getRedirectUrl();
    }
}
