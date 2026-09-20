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
 * a deploy has moved the backend out from under an open tab. Null or
 * commit-id-less GitProperties skips the header.
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
        String commitId = gitProperties == null ? null : gitProperties.getCommitId();
        if (commitId != null && !commitId.isBlank()) {
            response.setHeader(HEADER_NAME, commitId);
        }
        chain.doFilter(request, response);
    }
}
