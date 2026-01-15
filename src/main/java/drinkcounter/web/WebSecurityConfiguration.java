package drinkcounter.web;

import drinkcounter.UserService;
import drinkcounter.authentication.UserDetailsServiceImpl;
import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.servlet.PathRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    private final UserService userService;
    private final Customizer<HttpSecurity> oauth2LoginCustomizer;

    @Autowired
    public WebSecurityConfiguration(
            UserService userService,
            Customizer<HttpSecurity> oauth2LoginCustomizer) {
        this.userService = userService;
        this.oauth2LoginCustomizer = oauth2LoginCustomizer;
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
            .logout(logout -> logout
                .permitAll()
            )
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authorize -> {
                authorize
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
                        "/oauth2/**",
                        "/api/auth/google/one-tap"
                    ).permitAll()
                    .anyRequest().authenticated();
            });

        // Apply OAuth2 login configuration
        oauth2LoginCustomizer.customize(http);

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
}
