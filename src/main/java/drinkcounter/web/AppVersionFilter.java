package drinkcounter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.BuildProperties;

import java.io.IOException;

/**
 * Stamps every response with the running build's version, so sw.js can tell
 * when a deploy has moved the backend out from under an already-open tab.
 * Null BuildProperties (an IDE run that skips the build-info Maven step)
 * means no header - see GlobalControllerAdvice.applicationVersion().
 */
public class AppVersionFilter extends HttpFilter {

    static final String HEADER_NAME = "X-App-Version";

    private final BuildProperties buildProperties;

    public AppVersionFilter(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (buildProperties != null) {
            response.setHeader(HEADER_NAME, buildProperties.getVersion());
        }
        chain.doFilter(request, response);
    }
}
