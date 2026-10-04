package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringSecurityCurrentUserImplTest {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private final UserService userService = mock(UserService.class);
    private final SpringSecurityCurrentUserImpl currentUser = new SpringSecurityCurrentUserImpl(userService);
    private final User user = new User();

    @AfterEach
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void resolvesTheUserOfAFormLogin() {
        when(userService.getUser(42)).thenReturn(user);
        DrinkcounterUserDetails principal = new DrinkcounterUserDetails("user@example.com", "", true, true, true, true, AUTHORITIES, 42);
        signIn(new UsernamePasswordAuthenticationToken(principal, null, AUTHORITIES));

        assertSame(user, currentUser.getUser());
    }

    @Test
    public void resolvesTheUserOfAGoogleLogin() {
        when(userService.getUser(42)).thenReturn(user);
        OidcIdToken idToken = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60),
                Map.of("sub", "google-sub", "email", "user@example.com"));
        DrinkcounterOidcUser principal = new DrinkcounterOidcUser(AUTHORITIES, idToken, null, new LoggedInUser(42, "user@example.com"));
        signIn(new OAuth2AuthenticationToken(principal, AUTHORITIES, "google"));

        assertSame(user, currentUser.getUser());
    }

    @Test
    public void aPrincipalWithoutALoggedInUserResolvesToNoUser() {
        DefaultOAuth2User principal = new DefaultOAuth2User(AUTHORITIES, Map.of("email", "user@example.com"), "email");
        signIn(new OAuth2AuthenticationToken(principal, AUTHORITIES, "google"));

        assertNull(currentUser.getUser());
    }

    private static void signIn(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
