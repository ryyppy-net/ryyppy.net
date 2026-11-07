package drinkcounter.authentication.authorizationserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Registered apps, authorizations with their tokens, and consents, in the tables of the
 * {@code V13__oauth2_authorization_server} migration. Railway sleeps and restarts the app, so
 * none of this can live in memory.
 */
@Configuration
public class AuthorizationStorageConfiguration {

    @Bean
    public RegisteredClientRepository registeredClientRepository(JdbcTemplate jdbcTemplate) {
        return new JdbcRegisteredClientRepository(jdbcTemplate);
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClients) {
        JsonMapper json = AuthorizationJson.mapper();
        JdbcOAuth2AuthorizationService service = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClients);
        service.setAuthorizationRowMapper(new JsonMapperOAuth2AuthorizationRowMapper(registeredClients, json));
        service.setAuthorizationParametersMapper(new JsonMapperOAuth2AuthorizationParametersMapper(json));
        return service;
    }

    @Bean
    public OAuth2AuthorizationConsentService authorizationConsentService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClients) {
        return new JdbcOAuth2AuthorizationConsentService(jdbcTemplate, registeredClients);
    }
}
