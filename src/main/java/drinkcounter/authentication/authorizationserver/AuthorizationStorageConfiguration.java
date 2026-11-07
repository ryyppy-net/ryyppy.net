package drinkcounter.authentication.authorizationserver;

import org.springaicommunity.mcp.security.common.url.DefaultUrlValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdMetadataDocumentRegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.DefaultClientMetadataValidator;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationParametersMapper;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService.JsonMapperOAuth2AuthorizationRowMapper;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import tools.jackson.databind.json.JsonMapper;

/**
 * Apps are read from their client metadata documents. Authorizations with their tokens are in the
 * {@code oauth2_authorization} table: Railway sleeps and restarts the app, so they cannot live in memory.
 */
@Configuration
public class AuthorizationStorageConfiguration {

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        ClientIdMetadataDocumentRegisteredClientRepository repository = new ClientIdMetadataDocumentRegisteredClientRepository();
        // Desktop and command-line MCP clients receive the answer on a loopback http address.
        repository.setMetadataValidator(new DefaultClientMetadataValidator(new DefaultUrlValidator(true)));
        return repository;
    }

    @Bean
    public OAuth2AuthorizationService authorizationService(JdbcTemplate jdbcTemplate, RegisteredClientRepository registeredClients) {
        JsonMapper json = AuthorizationJson.mapper();
        JdbcOAuth2AuthorizationService service = new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClients);
        service.setAuthorizationRowMapper(new JsonMapperOAuth2AuthorizationRowMapper(registeredClients, json));
        service.setAuthorizationParametersMapper(new JsonMapperOAuth2AuthorizationParametersMapper(json));
        return service;
    }
}
