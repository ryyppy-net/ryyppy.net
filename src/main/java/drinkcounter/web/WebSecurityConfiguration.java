package drinkcounter.web;

import drinkcounter.UserService;
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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class WebSecurityConfiguration {

    private final UserService userService;

    @Autowired
    public WebSecurityConfiguration(UserService userService) {
        this.userService = userService;
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
                    "/API/passphrase/**"
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
}
