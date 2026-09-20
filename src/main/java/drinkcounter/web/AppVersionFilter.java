package drinkcounter.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.info.GitProperties;

import java.io.IOException;

/**
 * Stamps every response with the running commit hash, so sw.js can tell when
 * a deploy has moved the backend out from under an open tab - the Maven
 * version in pom.xml only moves on a deliberate release, so it can't tell
 * deploys apart. GitProperties has no commit id when the build ran without
 * .git (the Docker build stage, where the plugin still emits an empty
 * file rather than none); BuildProperties' git.revision (see pom.xml) is its
 * fallback there, blank or "unset" when neither applies.
 */
public class AppVersionFilter extends HttpFilter {

    static final String HEADER_NAME = "X-App-Version";
    private static final String UNSET_SENTINEL = "unset";

    private final GitProperties gitProperties;
    private final BuildProperties buildProperties;

    public AppVersionFilter(GitProperties gitProperties, BuildProperties buildProperties) {
        this.gitProperties = gitProperties;
        this.buildProperties = buildProperties;
    }

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String revision = revision();
        if (revision != null && !revision.isBlank() && !revision.equals(UNSET_SENTINEL)) {
            response.setHeader(HEADER_NAME, revision);
        }
        chain.doFilter(request, response);
    }

    private String revision() {
        String commitId = gitProperties == null ? null : gitProperties.getCommitId();
        if (commitId != null && !commitId.isBlank()) {
            return commitId;
        }
        return buildProperties == null ? null : buildProperties.get("git.revision");
    }
}
