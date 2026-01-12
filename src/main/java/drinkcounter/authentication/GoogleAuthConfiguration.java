package drinkcounter.authentication;

import drinkcounter.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

/**
 * Configuration for Google OAuth2 authentication.
 * Only loaded when google.auth.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = "google.auth.enabled", havingValue = "true", matchIfMissing = false)
public class GoogleAuthConfiguration {

    /**
     * Provides a customizer that configures OAuth2 login for the security filter chain.
     * This allows WebSecurityConfiguration to conditionally apply OAuth2 configuration.
     */
    @Bean
    public Customizer<HttpSecurity> oauth2LoginCustomizer(
            CustomOAuth2UserService customOAuth2UserService,
            OidcUserService oidcUserService) {
        return http -> {
            try {
                http.oauth2Login(oauth2 -> oauth2
                    .loginPage("/ui/login")
                    .userInfoEndpoint(userInfo -> userInfo
                        .oidcUserService(oidcUserService)
                        .userService(customOAuth2UserService)
                    )
                    .defaultSuccessUrl("/app/index.html", true)
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure OAuth2 login", e);
            }
        };
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
     * Creates the custom OAuth2 user service for Google login.
     */
    @Bean
    public CustomOAuth2UserService customOAuth2UserService(UserService userService) {
        return new CustomOAuth2UserService(userService);
    }
}
