package drinkcounter.authentication.authorizationserver;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The OAuth 2.1 authorization server that lets an LLM app act as a signed-in user on {@code /mcp}.
 * An app's client id is the https URL of its client metadata document, and every authorization goes
 * through the user's sign-in.
 * The issuer is each request's own origin, so PR previews issue their own tokens.
 */
@Configuration
public class AuthorizationServerConfiguration {

    static final String MCP_PATH = "/mcp";
    static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource" + MCP_PATH;

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http,
            RegisteredClientRepository registeredClients, AuthorizationServerSettings settings, JWKSource<SecurityContext> jwkSource)
            throws Exception {
        http
            .with(McpAuthorizationServerConfigurer.mcpAuthorizationServer(), mcp -> mcp
                .cimd(true)
                .dynamicClientRegistration(false)
                .authorizationServer(authorizationServer -> {
                    http.securityMatcher(authorizationServer.getEndpointsMatcher());
                    authorizationServer
                        .tokenGenerator(new DelegatingOAuth2TokenGenerator(accessTokens(jwkSource), new OAuth2AccessTokenGenerator(), new RefreshTokenGenerator()))
                        .clientAuthentication(clientAuthentication -> clientAuthentication
                            .authenticationConverter(new PublicClientAuthentication.Converter(PathPatternRequestMatcher.withDefaults()
                                .matcher(HttpMethod.POST, settings.getTokenRevocationEndpoint())))
                            .authenticationProvider(new PublicClientAuthentication.Provider(registeredClients)))
                        .tokenIntrospectionEndpoint(introspection -> introspection
                            .introspectionResponseHandler(new OwnTokenIntrospectionResponseHandler()));
                }))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/ui/login"),
                PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, settings.getAuthorizationEndpoint())));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mcpFilterChain(HttpSecurity http) throws Exception {
        BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
        entryPoint.setResourceMetadataParameterResolver(request ->
            ServletUriComponentsBuilder.fromContextPath(request).path(PROTECTED_RESOURCE_METADATA_PATH).toUriString());
        http
            .securityMatcher(MCP_PATH, MCP_PATH + "/**", PROTECTED_RESOURCE_METADATA_PATH)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PROTECTED_RESOURCE_METADATA_PATH).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(resourceServer -> resourceServer
                .authenticationEntryPoint(entryPoint)
                .jwt(Customizer.withDefaults())
                .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(resource -> resource
                    .authorizationServer(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString())
                    .tlsClientCertificateBoundAccessTokens(false))))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(@Value("${auth.jwt.signing-key:}") String signingKey) {
        return new ImmutableJWKSet<>(new JWKSet(SigningKey.of(signingKey)));
    }

    private static JwtGenerator accessTokens(JWKSource<SecurityContext> jwkSource) {
        JwtGenerator generator = new JwtGenerator(new NimbusJwtEncoder(jwkSource));
        generator.setJwtCustomizer(new UserIdSubject());
        return generator;
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
