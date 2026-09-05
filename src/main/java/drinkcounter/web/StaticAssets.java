package drinkcounter.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The fingerprinted URL scheme for static assets.
 *
 * <p>Every file under {@code classpath:/public/} is reachable twice: at its
 * plain path ({@code /static/js/common.js}) and under a build-specific prefix
 * ({@code /v/<build id>/static/js/common.js}). Only the second one is worth
 * caching hard - the prefix changes with every build, so a deploy invalidates
 * the whole set by handing out different URLs, and the browser never has to
 * ask whether its copy is still current.
 *
 * <p>The plain paths stay in place for the URLs we can't rewrite (the ones
 * hard-coded in JavaScript and CSS, plus old bookmarks); they are served with
 * a short max-age instead. See {@link WebConfiguration} for both mounts.
 */
@Component
public class StaticAssets {

    /** Mount point of the fingerprinted copy of {@code classpath:/public/}. */
    public static final String VERSIONED_PREFIX = "/v";

    /** The AngularJS app shell, relative to {@code classpath:/public/}. */
    public static final String APP_INDEX = "/app/index.html";

    private final String buildId;

    public StaticAssets(@Value("${application.build-id}") String buildId) {
        this.buildId = buildId;
    }

    /** The token that makes asset URLs unique per build. */
    public String buildId() {
        return buildId;
    }

    /** The fingerprinted URL of the app shell, e.g. {@code /v/20260905120000/app/index.html}. */
    public String frontPageUrl() {
        return VERSIONED_PREFIX + "/" + buildId + APP_INDEX;
    }

    /**
     * Redirect to the app shell's front page route. Carries an explicit
     * fragment, so don't use it where the browser should keep the one it
     * already had (a bookmark to an older build, say).
     */
    public String frontPageRedirect() {
        return "redirect:" + frontPageUrl() + "#/";
    }
}
