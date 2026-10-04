package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringSecurityCurrentUserImplTest {

    private static final List<SimpleGrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private UserService userService;
    private SpringSecurityCurrentUserImpl currentUser;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        currentUser = new SpringSecurityCurrentUserImpl();
        ReflectionTestUtils.setField(currentUser, "userService", userService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesUserFromDrinkcounterUserDetails() {
        User user = new User();
        when(userService.getUser(7)).thenReturn(user);
        DrinkcounterUserDetails details = new DrinkcounterUserDetails("a@b.c", "", true, true, true, true, AUTHORITIES, 7);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(details, null, AUTHORITIES));

        assertSame(user, currentUser.getUser());
    }

    @Test
    void resolvesUserFromOAuth2UserSessionsCreatedBeforeUnification() {
        User user = new User();
        when(userService.getUser(9)).thenReturn(user);
        DefaultOAuth2User principal = new DefaultOAuth2User(AUTHORITIES, Map.of("userId", 9, "sub", "s"), "sub");
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, AUTHORITIES, "google"));

        assertSame(user, currentUser.getUser());
    }

    @Test
    void returnsNullForOAuth2UserWithoutUserId() {
        DefaultOAuth2User principal = new DefaultOAuth2User(AUTHORITIES, Map.of("sub", "s"), "sub");
        SecurityContextHolder.getContext().setAuthentication(
                new OAuth2AuthenticationToken(principal, AUTHORITIES, "google"));

        assertNull(currentUser.getUser());
    }
}
