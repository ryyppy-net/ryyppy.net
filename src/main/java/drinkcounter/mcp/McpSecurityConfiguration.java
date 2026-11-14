package drinkcounter.mcp;

import static org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer.mcpAuthorizationServer;
import static org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2;
import static org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR;

import java.util.Set;
import org.springaicommunity.mcp.security.authorizationserver.config.LocalhostWildcardPortValidator;
import org.springaicommunity.mcp.security.common.url.DefaultUrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdMetadataDocumentRegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.DefaultClientMetadataValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * The MCP server at {@code /mcp} and the OAuth2 authorization server that issues its tokens.
 * MCP clients identify themselves with a Client ID Metadata Document; users sign in on the
 * app's own login page.
 */
@Configuration
public class McpSecurityConfiguration {

    private final String issuer;

    public McpSecurityConfiguration(@Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer) {
        this.issuer = issuer;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain mcpAuthorizationServerFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .with(mcpAuthorizationServer(), mcp -> mcp
                .dynamicClientRegistration(false)
                .cimd(true)
                // Claude Code registers http://localhost/callback and listens on a random port.
                .authorizationCodeRequestValidator(
                        new LocalhostWildcardPortValidator().andThen(DEFAULT_SCOPE_VALIDATOR))
                .authorizationServer(authorizationServer -> http.securityMatcher(new OrRequestMatcher(
                        authorizationServer.getEndpointsMatcher(),
                        PathPatternRequestMatcher.withDefaults().matcher("/.well-known/openid-configuration")))))
            .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                    new LoginUrlAuthenticationEntryPoint("/ui/login"), browserRequests()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mcpServerFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http
            .securityMatcher("/mcp", "/.well-known/oauth-protected-resource/**")
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            // Clients authenticate with a bearer token, never a cookie, so there is no CSRF to guard.
            .csrf(CsrfConfigurer::disable)
            // The issuer is this app, so its keys are read in-process rather than fetched over HTTP.
            .with(mcpServerOAuth2(), mcp -> mcp.authorizationServer(issuer).jwtDecoder(jwtDecoder));
        return http.build();
    }

    @Bean
    public ClientIdMetadataDocumentRegisteredClientRepository clientIdMetadataDocumentRegisteredClientRepository() {
        ClientIdMetadataDocumentRegisteredClientRepository repository = new ClientIdMetadataDocumentRegisteredClientRepository();
        // Native MCP clients such as Claude Code redirect to http://localhost or http://127.0.0.1.
        repository.setMetadataValidator(new DefaultClientMetadataValidator(new DefaultUrlValidator(true)));
        return repository;
    }

    private static MediaTypeRequestMatcher browserRequests() {
        MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return matcher;
    }
}
