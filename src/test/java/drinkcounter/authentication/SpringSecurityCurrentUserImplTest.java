package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringSecurityCurrentUserImplTest {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private UserService userService;
    private SpringSecurityCurrentUserImpl currentUser;
    private User user;

    @BeforeEach
    public void setUp() {
        userService = mock(UserService.class);
        currentUser = new SpringSecurityCurrentUserImpl();
        ReflectionTestUtils.setField(currentUser, "userService", userService);
        user = new User();
        user.setId(42);
        when(userService.getUser(42)).thenReturn(user);
    }

    @AfterEach
    public void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void resolvesFormLoginPrincipal() {
        DrinkcounterUserDetails principal = new DrinkcounterUserDetails(
                "user@example.com", "", true, true, true, true, AUTHORITIES, 42);
        authenticate(new UsernamePasswordAuthenticationToken(principal, null, AUTHORITIES));

        assertSame(user, currentUser.getUser());
    }

    @Test
    public void resolvesOAuth2Principal() {
        OAuth2User principal = new DrinkcounterOAuth2User(
                AUTHORITIES, Map.of("email", "user@example.com"), "email", 42);
        authenticate(new OAuth2AuthenticationToken(principal, AUTHORITIES, "google"));

        assertSame(user, currentUser.getUser());
    }

    @Test
    public void resolvesPlainOAuth2UserFromUserIdAttribute() {
        OAuth2User principal = new DefaultOAuth2User(
                AUTHORITIES, Map.of("email", "user@example.com", "userId", 42), "email");
        authenticate(new OAuth2AuthenticationToken(principal, AUTHORITIES, "google"));

        assertSame(user, currentUser.getUser());
    }

    @Test
    public void returnsNullForAnonymousUser() {
        authenticate(new AnonymousAuthenticationToken(
                "key", "anonymousUser", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertNull(currentUser.getUser());
    }

    private static void authenticate(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
