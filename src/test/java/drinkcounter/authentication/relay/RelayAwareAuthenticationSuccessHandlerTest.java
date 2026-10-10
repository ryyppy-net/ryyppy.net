package drinkcounter.authentication.relay;

import drinkcounter.authentication.GoogleIdentityLinkingService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RelayAwareAuthenticationSuccessHandlerTest {

    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_USER"));

    private AuthRelayTokenService tokenService;
    private GoogleIdentityLinkingService identityLinkingService;
    private RelayAwareAuthenticationSuccessHandler handler;
    private OAuth2AuthenticationToken authentication;

    @BeforeEach
    public void setUp() {
        tokenService = mock(AuthRelayTokenService.class);
        identityLinkingService = mock(GoogleIdentityLinkingService.class);
        handler = new RelayAwareAuthenticationSuccessHandler(tokenService, identityLinkingService);
        DefaultOAuth2User oauth2User = new DefaultOAuth2User(AUTHORITIES,
                Map.of("sub", "google-sub", "email", "user@example.com", "name", "User", "userId", 42), "email");
        authentication = new OAuth2AuthenticationToken(oauth2User, AUTHORITIES, "google");
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void directLoginEstablishesTheSameSessionAsOneTap() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        // Controllers only accept a DrinkcounterUserDetails principal, so the OAuth2 one must be replaced.
        verify(identityLinkingService).establishSession(42, "user@example.com", request);
        assertEquals("/app/index.html", response.getRedirectedUrl());
    }

    @Test
    public void relayedLoginHandsOffWithoutSigningInOnTheHub() throws Exception {
        String returnTo = "https://pr-123.up.railway.app";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.getSession(true).setAttribute(AuthRelayController.RETURN_TO_SESSION_ATTR, returnTo);
        MockHttpServletResponse response = new MockHttpServletResponse();
        // As OAuth2LoginAuthenticationFilter does before it calls the success handler.
        SecurityContext context = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        when(tokenService.mint("google-sub", "user@example.com", "User", returnTo)).thenReturn("token");

        handler.onAuthenticationSuccess(request, response, authentication);

        // The user signs in on the environment that started the relay, not on the hub.
        verify(identityLinkingService, never()).establishSession(anyInt(), anyString(), any());
        assertNull(request.getSession().getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY));
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        // That environment redeems the token to establish its own session.
        assertTrue(response.getRedirectedUrl().startsWith(returnTo + "/api/auth/relay/complete?token="));
    }
}
