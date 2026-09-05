package drinkcounter.authentication;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Builds the scheme://host[:port] origin for a request, or for the page that referred it - used
 * to figure out which environment a Google sign-in actually started on, so the result can be
 * relayed back there (see GoogleOneTapController and AuthRelayController).
 */
public final class Origins {

    private Origins() {
    }

    public static String of(HttpServletRequest request) {
        return build(request.getScheme(), request.getServerName(), request.getServerPort());
    }

    /**
     * The origin of the Referer header, or null if it's missing or unparseable. There's no other
     * way to learn which environment a cross-origin form POST (e.g. Google One Tap's login_uri)
     * actually came from.
     */
    public static String ofReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(referer);
            if (uri.getScheme() == null || uri.getHost() == null) {
                return null;
            }
            return build(uri.getScheme(), uri.getHost(), uri.getPort());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static String build(String scheme, String host, int port) {
        boolean defaultPort = port == -1
                || ("http".equals(scheme) && port == 80)
                || ("https".equals(scheme) && port == 443);
        return defaultPort ? scheme + "://" + host : scheme + "://" + host + ":" + port;
    }
}
