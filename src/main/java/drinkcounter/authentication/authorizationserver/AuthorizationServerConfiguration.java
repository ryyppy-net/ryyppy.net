package drinkcounter.authentication.authorizationserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationProvider;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientRegistrationAuthenticationValidator;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeRequestAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The OAuth 2.1 authorization server that lets an LLM app act as a signed-in user on {@code /mcp}.
 * Any app can register itself, and every authorization goes through the user's sign-in and consent.
 * The issuer is each request's own origin, so PR previews issue their own tokens.
 */
@Configuration
public class AuthorizationServerConfiguration {

    public static final String MCP_SCOPE = "mcp";

    static final String MCP_PATH = "/mcp";
    static final String PROTECTED_RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource" + MCP_PATH;
    static final String CONSENT_PAGE = "/ui/consent";

    @Bean
    @Order(1)
    public SecurityFilterChain authorizationServerFilterChain(HttpSecurity http,
            RegisteredClientRepository registeredClients, AuthorizationServerSettings settings) throws Exception {
        http
            .oauth2AuthorizationServer(authorizationServer -> {
                http.securityMatcher(authorizationServer.getEndpointsMatcher());
                authorizationServer
                    .tokenGenerator(new DelegatingOAuth2TokenGenerator(new OAuth2AccessTokenGenerator(), new RefreshTokenGenerator()))
                    .authorizationEndpoint(endpoint -> endpoint
                        .consentPage(CONSENT_PAGE)
                        .authorizationRequestConverters(converters -> converters.replaceAll(converter ->
                            converter instanceof OAuth2AuthorizationCodeRequestAuthenticationConverter
                                ? new DefaultScopeAuthorizationRequestConverter() : converter)))
                    .clientAuthentication(clientAuthentication -> clientAuthentication
                        .authenticationConverter(new PublicClientAuthentication.Converter(PathPatternRequestMatcher.withDefaults()
                            .matcher(HttpMethod.POST, settings.getTokenRevocationEndpoint())))
                        .authenticationProvider(new PublicClientAuthentication.Provider(registeredClients)))
                    .tokenIntrospectionEndpoint(introspection -> introspection
                        .introspectionResponseHandler(new OwnTokenIntrospectionResponseHandler()))
                    .clientRegistrationEndpoint(registration -> registration
                        .openRegistrationAllowed(true)
                        .authenticationProviders(providers -> providers.forEach(provider -> {
                            if (provider instanceof OAuth2ClientRegistrationAuthenticationProvider registrationProvider) {
                                registrationProvider.setRegisteredClientConverter(new ClientRegistrationPolicy());
                                // Requested scopes are replaced by the policy, so asking for one is not an error.
                                registrationProvider.setAuthenticationValidator(
                                    OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_REDIRECT_URI_VALIDATOR
                                        .andThen(OAuth2ClientRegistrationAuthenticationValidator.DEFAULT_JWK_SET_URI_VALIDATOR)
                                        .andThen(OAuth2ClientRegistrationAuthenticationValidator.SIMPLE_SCOPE_VALIDATOR));
                            }
                        })));
            })
            // Registration is open, so this server issues no initial access tokens to accept.
            .oauth2ResourceServer(resourceServer -> resourceServer.jwt(jwt -> jwt.decoder(token -> {
                throw new BadJwtException("No bearer token is accepted here");
            })))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.POST, settings.getClientRegistrationEndpoint()).permitAll()
                .anyRequest().authenticated())
            .requestCache(cache -> cache.requestCache(PendingAuthorization.requestCache()))
            .exceptionHandling(exceptions -> exceptions.defaultAuthenticationEntryPointFor(
                new LoginUrlAuthenticationEntryPoint("/ui/login?connect"),
                PendingAuthorization.AUTHORIZATION_REQUEST));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain mcpFilterChain(HttpSecurity http, OAuth2AuthorizationService authorizations) throws Exception {
        BearerTokenAuthenticationEntryPoint entryPoint = new BearerTokenAuthenticationEntryPoint();
        entryPoint.setResourceMetadataParameterResolver(request ->
            ServletUriComponentsBuilder.fromContextPath(request).path(PROTECTED_RESOURCE_METADATA_PATH).toUriString());
        http
            .securityMatcher(MCP_PATH, MCP_PATH + "/**", PROTECTED_RESOURCE_METADATA_PATH)
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(PROTECTED_RESOURCE_METADATA_PATH).permitAll()
                .anyRequest().hasAuthority("SCOPE_" + MCP_SCOPE))
            .oauth2ResourceServer(resourceServer -> resourceServer
                .authenticationEntryPoint(entryPoint)
                .opaqueToken(opaqueToken -> opaqueToken.authenticationManager(new AccessTokenAuthenticationManager(authorizations)))
                .protectedResourceMetadata(metadata -> metadata.protectedResourceMetadataCustomizer(resource -> resource
                    .authorizationServer(ServletUriComponentsBuilder.fromCurrentContextPath().toUriString())
                    .scope(MCP_SCOPE)
                    .tlsClientCertificateBoundAccessTokens(false))))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .csrf(csrf -> csrf.disable());
        return http.build();
    }

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
