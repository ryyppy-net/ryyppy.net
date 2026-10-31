package drinkcounter.analytics;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * PostHog settings. An empty {@code key} turns analytics off: no snippet is rendered, no events
 * are sent and the {@code /ph} proxy answers 404.
 *
 * @param key         the project API key, which is public
 * @param host        ingestion host, the EU cloud
 * @param assetsHost  host serving posthog-js and its lazily loaded extensions
 * @param environment sent with every event so one project can hold all deployments
 */
@ConfigurationProperties("posthog")
public record PostHogProperties(
        @DefaultValue("") String key,
        @DefaultValue("https://eu.i.posthog.com") String host,
        @DefaultValue("https://eu-assets.i.posthog.com") String assetsHost,
        @DefaultValue("local") String environment) {

    public boolean enabled() {
        return key != null && !key.isBlank();
    }
}
