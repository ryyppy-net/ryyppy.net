package drinkcounter.authentication;

import drinkcounter.model.User;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrinkcounterOidcUserServiceTest {

    private final OidcUserService delegate = mock(OidcUserService.class);
    private final GoogleIdentityLinkingService identityLinkingService = mock(GoogleIdentityLinkingService.class);
    private final DrinkcounterOidcUserService service = new DrinkcounterOidcUserService(delegate, identityLinkingService);
    private final OidcUserRequest request = mock(OidcUserRequest.class);

    @Test
    public void googleLoginReturnsADrinkcounterPrincipalForTheLinkedUser() {
        OidcIdToken idToken = idToken(Map.of("sub", "google-sub", "email", "user@example.com", "name", "User"));
        when(delegate.loadUser(request)).thenReturn(new DefaultOidcUser(List.of(new SimpleGrantedAuthority("OIDC_USER")), idToken));
        User user = new User();
        user.setId(42);
        when(identityLinkingService.findOrCreateUser("google-sub", "user@example.com", "User", null, null)).thenReturn(user);

        OidcUser principal = service.loadUser(request);

        DrinkcounterOidcUser oidcPrincipal = assertInstanceOf(DrinkcounterOidcUser.class, principal);
        assertEquals(new LoggedInUser(42, "user@example.com"), oidcPrincipal.getLoggedInUser());
        assertEquals("user@example.com", oidcPrincipal.getName());
        assertSame(idToken, oidcPrincipal.getIdToken());
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_USER")), List.copyOf(oidcPrincipal.getAuthorities()));
    }

    @Test
    public void googleLoginWithoutAnEmailIsRejected() {
        when(delegate.loadUser(request)).thenReturn(new DefaultOidcUser(List.of(), idToken(Map.of("sub", "google-sub"))));

        assertThrows(OAuth2AuthenticationException.class, () -> service.loadUser(request));
    }

    private static OidcIdToken idToken(Map<String, Object> claims) {
        return new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), claims);
    }
}
