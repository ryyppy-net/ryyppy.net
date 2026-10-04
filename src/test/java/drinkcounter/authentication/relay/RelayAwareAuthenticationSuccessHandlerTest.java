package drinkcounter.authentication.relay;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RelayAwareAuthenticationSuccessHandlerTest {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private AuthRelayTokenService tokenService;
    private RelayAwareAuthenticationSuccessHandler handler;
    private OAuth2AuthenticationToken authentication;

    @BeforeEach
    public void setUp() {
        tokenService = mock(AuthRelayTokenService.class);
        handler = new RelayAwareAuthenticationSuccessHandler(tokenService);
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(AUTHORITIES,
                Map.of("sub", "google-sub", "email", "user@example.com", "name", "User", "userId", 42), "email");
        authentication = new OAuth2AuthenticationToken(oauth2User, AUTHORITIES, "google");
    }

    @Test
    public void directLoginGoesToTheApp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(tokenService, never()).mint(anyString(), anyString(), anyString(), anyString());
        assertEquals("/app/index.html", response.getRedirectedUrl());
    }

    @Test
    public void relayedLoginHandsOffToTheEnvironmentThatStartedIt() throws Exception {
        String returnTo = "https://pr-123.up.railway.app";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AuthRelayController.RETURN_TO_SESSION_ATTR, returnTo);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.mint("google-sub", "user@example.com", "User", returnTo)).thenReturn("token");

        handler.onAuthenticationSuccess(request, response, authentication);

        // That environment redeems the token to establish its own session.
        assertTrue(response.getRedirectedUrl().startsWith(returnTo + "/api/auth/relay/complete?token="));
    }
}
