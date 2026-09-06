package drinkcounter.authentication;

import drinkcounter.authentication.relay.AuthRelayTokenService;
import drinkcounter.authentication.relay.RelayAwareAuthenticationSuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

/**
 * Configuration for OAuth2/OIDC social authentication.
 * Supports any OAuth2/OIDC provider configured in application.yml
 * (Google, GitHub, Facebook, etc.).
 */
@Configuration
public class OAuth2Configuration {

    /**
     * Provides a customizer that configures OAuth2 login for the security filter chain.
     */
    @Bean
    public Customizer<HttpSecurity> oauth2LoginCustomizer(
            CustomOAuth2UserService customOAuth2UserService,
            OidcUserService oidcUserService,
            RelayAwareAuthenticationSuccessHandler relayAwareAuthenticationSuccessHandler) {
        return http -> {
            try {
                http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/ui/login")
                    .userInfoEndpoint(userInfo -> userInfo
                        .oidcUserService(oidcUserService)
                        .userService(customOAuth2UserService)
                    )
                    .successHandler(relayAwareAuthenticationSuccessHandler)
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure OAuth2 login", e);
            }
        };
    }

    /**
     * Completes classical OAuth2 login. When the login was started via the cross-environment
     * relay (see AuthRelayController), hands the verified identity back to the environment that
     * started it; otherwise this is an ordinary successful login on the current host.
     */
    @Bean
    public RelayAwareAuthenticationSuccessHandler relayAwareAuthenticationSuccessHandler(AuthRelayTokenService tokenService) {
        return new RelayAwareAuthenticationSuccessHandler(tokenService);
    }

    /**
     * Creates the OIDC user service that integrates with our custom OAuth2 user service.
     */
    @Bean
    public OidcUserService oidcUserService(CustomOAuth2UserService customOAuth2UserService) {
        OidcUserService oidcUserService = new OidcUserService();
        oidcUserService.setOauth2UserService(customOAuth2UserService);
        return oidcUserService;
    }

    /**
     * Creates the custom OAuth2 user service for social login.
     * Handles user lookup and creation for any OAuth2/OIDC provider.
     */
    @Bean
    public CustomOAuth2UserService customOAuth2UserService(GoogleIdentityLinkingService identityLinkingService) {
        return new CustomOAuth2UserService(identityLinkingService);
    }
}
