package drinkcounter.analytics;

import drinkcounter.model.User;

import java.util.LinkedHashMap;
import java.util.Map;

/** What PostHog knows about a person; sent by the page snippet and with every server event. */
public final class PersonProperties {

    private PersonProperties() {
    }

    public static Map<String, Object> of(User user) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("name", user.getName());
        properties.put("email", user.getEmail());
        properties.put("sex", user.getSex().name().toLowerCase());
        properties.put("weight", user.getWeight());
        properties.put("guest", user.isGuest());
        return properties;
    }

    /** PostHog's distinct id for a user. */
    public static String distinctId(User user) {
        return String.valueOf(user.getId());
    }
}
