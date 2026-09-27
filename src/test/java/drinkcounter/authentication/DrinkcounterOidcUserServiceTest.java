package drinkcounter.authentication;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DrinkcounterOidcUserServiceTest {

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService;
    private DrinkcounterOidcUserService oidcUserService;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() {
        oauth2UserService = mock(OAuth2UserService.class);
        oidcUserService = new DrinkcounterOidcUserService();
        oidcUserService.setOauth2UserService(oauth2UserService);
    }

    @Test
    public void principalCarriesUserIdFromUserInfo() {
        when(oauth2UserService.loadUser(any())).thenReturn(userInfo(Map.of("sub", "google-sub", "userId", 42)));

        OidcUser user = oidcUserService.loadUser(request());

        DrinkcounterPrincipal principal = assertInstanceOf(DrinkcounterPrincipal.class, user);
        assertEquals(42, principal.getUserId());
        assertEquals("google-sub", user.getName());
    }

    @Test
    public void userInfoWithoutUserIdStaysAPlainOidcUser() {
        when(oauth2UserService.loadUser(any())).thenReturn(userInfo(Map.of("sub", "google-sub")));

        OidcUser user = oidcUserService.loadUser(request());

        assertFalse(user instanceof DrinkcounterPrincipal);
    }

    private static OAuth2User userInfo(Map<String, Object> attributes) {
        return new DefaultOAuth2User(List.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, "sub");
    }

    private static OidcUserRequest request() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("https://ryyppy.net/login/oauth2/code/google")
                .authorizationUri("https://accounts.example.com/auth")
                .tokenUri("https://accounts.example.com/token")
                .userInfoUri("https://accounts.example.com/userinfo")
                .userNameAttributeName("sub")
                .build();
        Instant now = Instant.now();
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, "access-token", now, now.plusSeconds(60));
        OidcIdToken idToken = new OidcIdToken("id-token", now, now.plusSeconds(60), Map.of("sub", "google-sub"));
        return new OidcUserRequest(registration, accessToken, idToken);
    }
}
