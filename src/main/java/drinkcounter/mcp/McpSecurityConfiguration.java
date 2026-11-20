package drinkcounter.mcp;

import static org.springaicommunity.mcp.security.authorizationserver.config.McpAuthorizationServerConfigurer.mcpAuthorizationServer;
import static org.springaicommunity.mcp.security.server.config.McpServerOAuth2Configurer.mcpServerOAuth2;
import static org.springframework.security.oauth2.server.authorization.authentication.OAuth2AuthorizationCodeRequestAuthenticationValidator.DEFAULT_SCOPE_VALIDATOR;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Set;
import org.springaicommunity.mcp.security.authorizationserver.config.LocalhostWildcardPortValidator;
import org.springaicommunity.mcp.security.common.url.DefaultUrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.ClientIdMetadataDocumentRegisteredClientRepository;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.client.metadata.DefaultClientMetadataValidator;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
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

    /** The one scope: everything the MCP tools can do as the user. */
    public static final String MCP_SCOPE = "mcp";

    static final String CONSENT_PAGE = "/ui/mcp/consent";

    private final String issuer;

    public McpSecurityConfiguration(@Value("${spring.security.oauth2.authorizationserver.issuer}") String issuer) {
        this.issuer = issuer;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain mcpAuthorizationServerFilterChain(HttpSecurity http,
            ClientIdMetadataDocumentRegisteredClientRepository registeredClients) throws Exception {
        http
            .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
            .with(mcpAuthorizationServer(), mcp -> mcp
                .dynamicClientRegistration(false)
                .cimd(true)
                // Claude Code registers http://localhost/callback and listens on a random port.
                .authorizationCodeRequestValidator(
                        new LocalhostWildcardPortValidator().andThen(DEFAULT_SCOPE_VALIDATOR))
                .authorizationServer(authorizationServer -> {
                    authorizationServer.authorizationEndpoint(authorizationEndpoint -> authorizationEndpoint
                            .authorizationRequestConverters(converters -> converters.replaceAll(DefaultScopeAuthorizationRequestConverter::new))
                            .consentPage(CONSENT_PAGE));
                    // Claude's hosted apps use a client metadata document only if "none" is listed.
                    // Setting the customizer replaces the library's, so it repeats the CIMD claim.
                    authorizationServer.authorizationServerMetadataEndpoint(metadata -> metadata
                            .authorizationServerMetadataCustomizer(builder -> builder
                                    .claim("client_id_metadata_document_supported", true)
                                    .tokenEndpointAuthenticationMethods(methods -> methods.add("none"))));
                    http.securityMatcher(new OrRequestMatcher(
                            authorizationServer.getEndpointsMatcher(),
                            PathPatternRequestMatcher.withDefaults().matcher("/.well-known/openid-configuration")));
                    // MCP clients are public clients, and they stay signed in by refreshing their tokens.
                    authorizationServer
                            .tokenGenerator(new DelegatingOAuth2TokenGenerator(
                                    http.getSharedObject(OAuth2TokenGenerator.class), new PublicClientRefreshTokenGenerator()))
                            .clientAuthentication(clientAuthentication -> clientAuthentication
                                    .authenticationConverter(new PublicClientRefreshTokenAuthenticationConverter())
                                    .authenticationProvider(new PublicClientRefreshTokenAuthenticationProvider(registeredClients)));
                }))
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
            // As in the library's sample server: a token-less POST must get the 401 that starts MCP sign-in.
            .csrf(CsrfConfigurer::disable)
            // The issuer is this app, so its keys are read in-process rather than fetched over HTTP.
            .with(mcpServerOAuth2(), mcp -> mcp.authorizationServer(issuer).jwtDecoder(jwtDecoder)
                    // Setting the customizer replaces the library's, so it repeats the library's two claims.
                    .protectedResourceMetadataCustomizer(metadata -> metadata
                            .authorizationServer(issuer)
                            .resourceName("Spring MCP Resource Server")
                            .scope(MCP_SCOPE)));
        return http.build();
    }

    @Bean
    public ClientIdMetadataDocumentRegisteredClientRepository clientIdMetadataDocumentRegisteredClientRepository() {
        ClientIdMetadataDocumentRegisteredClientRepository repository = new ClientIdMetadataDocumentRegisteredClientRepository();
        // Native MCP clients such as Claude Code redirect to http://localhost or http://127.0.0.1.
        repository.setMetadataValidator(new DefaultClientMetadataValidator(new DefaultUrlValidator(true)));
        OAuth2ClientRegistrationRegisteredClientConverter converter = new OAuth2ClientRegistrationRegisteredClientConverter();
        // Public clients' refresh tokens must rotate. Each one lives a year from its issue, so a client
        // stays signed in until it goes unused for a year.
        converter.setTokenSettingsCustomizer(tokenSettings -> tokenSettings
                .reuseRefreshTokens(false)
                .refreshTokenTimeToLive(Duration.ofDays(365)));
        // Client metadata documents name no scopes, so every client is given the one scope there is.
        repository.setRegisteredClientConverter(registration -> RegisteredClient.from(converter.convert(registration))
                .scope(MCP_SCOPE)
                .build());
        return repository;
    }

    /**
     * Keeps authorizations, and so refresh tokens, in the database: Railway sleeps the service when
     * idle and every wake restores it from the image's checkpoint.
     */
    @Bean
    public OAuth2AuthorizationService oauth2AuthorizationService(JdbcOperations jdbcOperations,
            ClientIdMetadataDocumentRegisteredClientRepository registeredClients) {
        return new JdbcOAuth2AuthorizationService(jdbcOperations, new RegisteredClientsById(registeredClients));
    }

    /**
     * A Client ID Metadata Document client's id is its client_id URL, which this resolves again when
     * the repository no longer holds the client, such as after a restart.
     */
    private record RegisteredClientsById(RegisteredClientRepository registeredClients) implements RegisteredClientRepository {

        @Override
        public void save(RegisteredClient registeredClient) {
            registeredClients.save(registeredClient);
        }

        @Override
        public RegisteredClient findById(String id) {
            RegisteredClient client = registeredClients.findById(id);
            return client != null ? client : registeredClients.findByClientId(id);
        }

        @Override
        public RegisteredClient findByClientId(String clientId) {
            return registeredClients.findByClientId(clientId);
        }
    }

    /**
     * Signs the access tokens with AUTH_JWT_SIGNING_KEY, a PKCS#8 PEM RSA private key. Where it is
     * not set, such as locally and in PR previews, a key is generated as the context starts: on each
     * cold boot, or once per image where the CRaC checkpoint captures it.
     */
    @Bean
    public JWKSource<SecurityContext> mcpJwkSource(@Value("${auth.jwt.signing-key:}") String signingKey)
            throws GeneralSecurityException, JOSEException {
        RSAPrivateCrtKey privateKey = signingKey.isBlank() ? generatedKey() : (RSAPrivateCrtKey) RsaKeyConverters.pkcs8()
                .convert(new ByteArrayInputStream(signingKey.getBytes(StandardCharsets.US_ASCII)));
        RSAPublicKey publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA")
                .generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey).keyIDFromThumbprint().build();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    private static RSAPrivateCrtKey generatedKey() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return (RSAPrivateCrtKey) generator.generateKeyPair().getPrivate();
    }

    private static MediaTypeRequestMatcher browserRequests() {
        MediaTypeRequestMatcher matcher = new MediaTypeRequestMatcher(MediaType.TEXT_HTML);
        matcher.setIgnoredMediaTypes(Set.of(MediaType.ALL));
        return matcher;
    }
}
