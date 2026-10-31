package drinkcounter.analytics;

import drinkcounter.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class AnalyticsTest {

    private final PostHogHttp http = mock(PostHogHttp.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static User user() {
        User user = new User();
        user.setId(7);
        user.setName("Ville");
        user.setEmail("ville@example.com");
        user.setSex(User.Sex.FEMALE);
        user.setWeight(62.5f);
        return user;
    }

    private Analytics analytics(String key) {
        return new Analytics(new PostHogProperties(key, "https://eu.i.posthog.com", "https://eu-assets.i.posthog.com", "pr-12"),
                http, objectMapper);
    }

    @Test
    void sendsNothingWithoutAKey() {
        analytics("").capture(user(), "drink_added", Map.of());

        verifyNoInteractions(http);
    }

    @Test
    void sendsTheEventToTheCaptureEndpointWithPersonAndEnvironment() {
        analytics("phc_key").capture(user(), "drink_added", Map.of("alcohol_grams", 12.0f));

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(http).postJson(org.mockito.ArgumentMatchers.eq(URI.create("https://eu.i.posthog.com/capture/")), body.capture());
        JsonNode json = objectMapper.readTree(body.getValue());
        assertEquals("phc_key", json.get("api_key").asString());
        assertEquals("drink_added", json.get("event").asString());
        assertEquals("7", json.get("distinct_id").asString());
        assertEquals("pr-12", json.get("properties").get("environment").asString());
        assertEquals(12.0, json.get("properties").get("alcohol_grams").asDouble());
        JsonNode person = json.get("properties").get("$set");
        assertEquals("Ville", person.get("name").asString());
        assertEquals("ville@example.com", person.get("email").asString());
        assertEquals("female", person.get("sex").asString());
        assertEquals(62.5, person.get("weight").asDouble());
        assertFalse(person.get("guest").asBoolean());
    }

    @Test
    void sendsOnlyAfterTheTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();

        analytics("phc_key").capture(user(), "drink_added", Map.of());

        verify(http, never()).postJson(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(http).postJson(any(), any());
    }

    @Test
    void dropsTheEventWhenTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();

        analytics("phc_key").capture(user(), "drink_added", Map.of());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(http, never()).postJson(any(), any());
        assertTrue(TransactionSynchronizationManager.isSynchronizationActive());
    }
}
