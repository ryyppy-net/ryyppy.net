package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.withAccessToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import drinkcounter.authentication.DrinkcounterUserDetails;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

public class AccessTokenAuthenticationManagerTest {

    private final OAuth2AuthorizationService authorizations = mock(OAuth2AuthorizationService.class);
    private final AccessTokenAuthenticationManager manager = new AccessTokenAuthenticationManager(authorizations);

    @Test
    public void tokenSignsInAsTheUserWhoAuthorizedIt() {
        stub("token", withAccessToken(publicClient("app"), "token", Instant.now(), "ville@example.com", 42));

        Authentication authentication = manager.authenticate(new BearerTokenAuthenticationToken("token"));

        DrinkcounterUserDetails user = assertInstanceOf(DrinkcounterUserDetails.class, authentication.getPrincipal());
        assertEquals(42, user.getUserId());
        assertTrue(authentication.isAuthenticated());
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("SCOPE_mcp")));
        assertTrue(authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    public void expiredTokenIsRejected() {
        stub("token", withAccessToken(publicClient("app"), "token", Instant.now().minus(Duration.ofHours(2)), "ville@example.com", 42));

        assertThrows(InvalidBearerTokenException.class, () -> manager.authenticate(new BearerTokenAuthenticationToken("token")));
    }

    @Test
    public void revokedTokenIsRejected() {
        OAuth2Authorization authorization = withAccessToken(publicClient("app"), "token", Instant.now(), "ville@example.com", 42);
        stub("token", OAuth2Authorization.from(authorization).invalidate(authorization.getAccessToken().getToken()).build());

        assertThrows(InvalidBearerTokenException.class, () -> manager.authenticate(new BearerTokenAuthenticationToken("token")));
    }

    @Test
    public void unknownTokenIsRejected() {
        assertThrows(InvalidBearerTokenException.class, () -> manager.authenticate(new BearerTokenAuthenticationToken("unknown")));
    }

    private void stub(String token, OAuth2Authorization authorization) {
        when(authorizations.findByToken(token, OAuth2TokenType.ACCESS_TOKEN)).thenReturn(authorization);
    }
}
