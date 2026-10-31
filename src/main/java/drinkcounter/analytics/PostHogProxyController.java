package drinkcounter.analytics;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Serves PostHog's browser library and ingestion API from our own origin under {@code /ph}, so
 * ad blockers that list PostHog's domains do not drop them. Requests go to a fixed host; the
 * client's cookies are not forwarded.
 */
@Controller
public class PostHogProxyController {

    static final String PREFIX = "/ph";

    private static final Logger log = LoggerFactory.getLogger(PostHogProxyController.class);
    private static final int MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final Pattern SAFE_PATH = Pattern.compile("/[A-Za-z0-9._~/-]*");
    private static final List<String> ASSET_PREFIXES = List.of("/static/", "/array/");
    private static final List<String> REQUEST_HEADERS =
            List.of("Content-Type", "Content-Encoding", "Accept", "Accept-Encoding", "User-Agent");
    private static final List<String> RESPONSE_HEADERS =
            List.of("Content-Type", "Content-Encoding", "Cache-Control");

    private final PostHogProperties properties;
    private final PostHogHttp http;

    public PostHogProxyController(PostHogProperties properties, PostHogHttp http) {
        this.properties = properties;
        this.http = http;
    }

    @RequestMapping(value = PREFIX + "/**", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<byte[]> proxy(HttpServletRequest request) throws IOException {
        String path = request.getRequestURI().substring(PREFIX.length());
        if (!properties.enabled()) {
            return ResponseEntity.notFound().build();
        }
        if (path.isEmpty()) {
            path = "/";
        }
        if (!SAFE_PATH.matcher(path).matches() || path.contains("..")) {
            return ResponseEntity.badRequest().build();
        }

        byte[] body = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (body.length > MAX_BODY_BYTES) {
            return ResponseEntity.status(HttpStatus.CONTENT_TOO_LARGE).build();
        }

        URI uri;
        try {
            uri = upstreamUri(path, request.getQueryString());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        HttpRequest.Builder upstream = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .method(request.getMethod(), HttpRequest.BodyPublishers.ofByteArray(body))
                .header("X-Forwarded-For", request.getRemoteAddr());
        for (String name : REQUEST_HEADERS) {
            String value = request.getHeader(name);
            if (value != null) {
                upstream.header(name, value);
            }
        }

        HttpResponse<byte[]> response;
        try {
            response = http.send(upstream.build());
        } catch (IOException e) {
            log.debug("PostHog proxy failed: {}", e.toString());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        HttpHeaders headers = new HttpHeaders();
        for (String name : RESPONSE_HEADERS) {
            response.headers().firstValue(name).ifPresent(value -> headers.set(name, value));
        }
        return ResponseEntity.status(response.statusCode()).headers(headers).body(response.body());
    }

    private URI upstreamUri(String path, String query) {
        boolean asset = ASSET_PREFIXES.stream().anyMatch(path::startsWith);
        String base = asset ? properties.assetsHost() : properties.host();
        return URI.create(base + path + (query == null ? "" : "?" + query));
    }
}
