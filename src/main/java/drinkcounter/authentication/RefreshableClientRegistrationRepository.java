package drinkcounter.authentication;

import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Replaces Spring Boot's auto-configured (and {@code final}) InMemoryClientRegistrationRepository,
 * which can't be made to pick up a CRaC restore's real client-id/secret: it's built once at
 * startup from OAuth2ClientProperties, and being final, can't be wrapped in the CGLIB proxy
 * spring.cloud.refresh.extra-refreshable needs to rebuild a bean after restore (attempting that
 * fails hard: "Could not generate CGLIB subclass ... Cannot subclass final class
 * InMemoryClientRegistrationRepository").
 *
 * Reads the client-id/secret fresh via Environment on every call instead - the same fix already
 * applied to AuthRelayTokenService/AuthRelayController/GlobalControllerAdvice for the same
 * underlying problem (see application.yml's app.auth-relay-secret for the full explanation).
 */
class RefreshableClientRegistrationRepository implements ClientRegistrationRepository {

    private final Environment environment;

    RefreshableClientRegistrationRepository(Environment environment) {
        this.environment = environment;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {
        if (!"google".equals(registrationId)) {
            return null;
        }
        String clientId = environment.getProperty(
                "spring.security.oauth2.client.registration.google.client-id", "REPLACE_THIS");
        String clientSecret = environment.getProperty(
                "spring.security.oauth2.client.registration.google.client-secret", "REPLACE_THIS");
        return CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
