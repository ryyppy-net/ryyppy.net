package drinkcounter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.GitProperties;

import java.io.IOException;

/**
 * Stamps every response with the running commit hash, so sw.js can tell when
 * a deploy has moved the backend out from under an already-open tab. The
 * Maven version in pom.xml only moves on a deliberate release, so it can't
 * tell two deploys apart - the commit hash always does. Null GitProperties
 * (an IDE run that skips the git-commit-id Maven step) means no header.
 */
public class AppVersionFilter extends HttpFilter {

    static final String HEADER_NAME = "X-App-Version";

    private final GitProperties gitProperties;

    public AppVersionFilter(GitProperties gitProperties) {
        this.gitProperties = gitProperties;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (gitProperties != null) {
            response.setHeader(HEADER_NAME, gitProperties.getCommitId());
        }
        chain.doFilter(request, response);
    }
}
