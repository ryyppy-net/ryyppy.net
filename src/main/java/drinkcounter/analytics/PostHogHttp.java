package drinkcounter.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * The HTTP client for talking to PostHog. It is created on first use: the image's CRaC checkpoint
 * is taken after boot and cannot hold the client's open selector.
 */
@Component
public class PostHogHttp {

    private static final Logger log = LoggerFactory.getLogger(PostHogHttp.class);
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private HttpClient client;

    /** Sends a JSON body without waiting for the response; a failure is only logged. */
    public void postJson(URI uri, String json) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        client().sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        log.debug("PostHog capture failed: {}", error.toString());
                    } else if (response.statusCode() >= 400) {
                        log.debug("PostHog capture answered {}", response.statusCode());
                    }
                });
    }

    public HttpResponse<byte[]> send(HttpRequest request) throws IOException, InterruptedException {
        return client().send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private synchronized HttpClient client() {
        if (client == null) {
            client = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        }
        return client;
    }
}
