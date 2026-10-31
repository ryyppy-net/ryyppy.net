package drinkcounter.analytics;

import drinkcounter.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Sends server-side events to PostHog, so one event covers both front ends and the classic API.
 * Inside a transaction an event is sent only once it commits.
 */
@Service
public class Analytics {

    private final PostHogProperties properties;
    private final PostHogHttp http;
    private final ObjectMapper objectMapper;

    public Analytics(PostHogProperties properties, PostHogHttp http, ObjectMapper objectMapper) {
        this.properties = properties;
        this.http = http;
        this.objectMapper = objectMapper;
    }

    public void capture(User user, String event, Map<String, ?> eventProperties) {
        if (!properties.enabled()) {
            return;
        }
        String body = body(user, event, eventProperties);
        URI uri = URI.create(properties.host() + "/capture/");
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    http.postJson(uri, body);
                }
            });
        } else {
            http.postJson(uri, body);
        }
    }

    private String body(User user, String event, Map<String, ?> eventProperties) {
        Map<String, Object> eventBody = new LinkedHashMap<>(eventProperties);
        eventBody.put("environment", properties.environment());
        eventBody.put("$lib", "ryyppy.net");
        eventBody.put("$set", PersonProperties.of(user));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api_key", properties.key());
        body.put("event", event);
        body.put("distinct_id", PersonProperties.distinctId(user));
        body.put("timestamp", Instant.now().toString());
        body.put("properties", eventBody);
        return objectMapper.writeValueAsString(body);
    }
}
