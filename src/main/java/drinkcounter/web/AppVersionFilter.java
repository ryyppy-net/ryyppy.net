package drinkcounter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.BuildProperties;

import java.io.IOException;

/**
 * Stamps every response with the running commit hash, so sw.js can tell when
 * a deploy has moved the backend out from under an already-open tab. The
 * Maven version in pom.xml only moves on a deliberate release, so it can't
 * tell two deploys apart - the commit hash always does. It comes through as
 * BuildProperties' git.revision (see pom.xml); blank or the pom's "unset"
 * default means that build never set -Dgit.revision, so no header goes out,
 * same as a null BuildProperties.
 */
public class AppVersionFilter extends HttpFilter {

    static final String HEADER_NAME = "X-App-Version";
    private static final String UNSET_SENTINEL = "unset";

    private final BuildProperties buildProperties;

    public AppVersionFilter(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String revision = buildProperties == null ? null : buildProperties.get("git.revision");
        if (revision != null && !revision.isBlank() && !revision.equals(UNSET_SENTINEL)) {
            response.setHeader(HEADER_NAME, revision);
        }
        chain.doFilter(request, response);
    }
}
