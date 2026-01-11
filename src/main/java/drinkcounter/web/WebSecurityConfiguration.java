package drinkcounter.web;

import drinkcounter.UserService;
import drinkcounter.authentication.CustomOAuth2UserService;
import drinkcounter.authentication.UserDetailsServiceImpl;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    private final UserService userService;
    private final CustomOAuth2UserService customOAuth2UserService;

    @Autowired
    public WebSecurityConfiguration(UserService userService, CustomOAuth2UserService customOAuth2UserService) {
        this.userService = userService;
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .formLogin(form -> form
                .loginPage("/ui/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/app/index.html", true)
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/ui/login")
                .userInfoEndpoint(userInfo -> userInfo
                    .oidcUserService(oidcUserService())
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/app/index.html", true)
            )
            .logout(logout -> logout
                .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> authorize
                .dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll()
                .requestMatchers(PathRequest.toStaticResources().atCommonLocations()).permitAll()
                .requestMatchers(
                    "/login",
                    "/ui/login",
                    "/static/**",
                    "/ui/newuser",
                    "/ui/addUser",
                    "/ui/checkEmail*",
                    "/ui/timezone/*",
                    "/app/css/**",
                    "/API/passphrase/**",
                    "/oauth2/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return new UserDetailsServiceImpl(userService);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public OidcUserService oidcUserService() {
        OidcUserService oidcUserService = new OidcUserService();
        oidcUserService.setOauth2UserService(customOAuth2UserService);
        return oidcUserService;
    }
}
