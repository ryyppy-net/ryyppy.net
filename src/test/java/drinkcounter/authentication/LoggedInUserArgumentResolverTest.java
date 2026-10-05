package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LoggedInUserArgumentResolverTest {

    private final UserService userService = mock(UserService.class);
    private final LoggedInUserArgumentResolver resolver = new LoggedInUserArgumentResolver(userService);

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void loadsTheUserNamedByThePrincipal() {
        User user = new User();
        when(userService.getUser(42)).thenReturn(user);
        DrinkcounterUserDetails principal = new DrinkcounterUserDetails("user@example.com", "",
                true, true, true, true, List.of(new SimpleGrantedAuthority("ROLE_USER")), 42);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(principal, null));

        assertSame(user, resolver.resolveArgument(null, null, null, null));
    }

    @Test
    public void otherPrincipalIsRejectedWithoutLookup() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("anonymousUser", null));

        assertThrows(IllegalStateException.class, () -> resolver.resolveArgument(null, null, null, null));
        verify(userService, never()).getUser(anyInt());
    }

    @Test
    public void missingAuthenticationIsRejected() {
        assertThrows(IllegalStateException.class, () -> resolver.resolveArgument(null, null, null, null));
    }
}
