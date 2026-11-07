package drinkcounter.authentication.authorizationserver;

import drinkcounter.authentication.DrinkcounterUserDetails;
import java.security.Principal;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

/** Registered apps and authorizations as the authorization server stores them. */
public final class AuthorizationFixtures {

    public static final String REDIRECT_URI = "http://127.0.0.1:33418/callback";

    private AuthorizationFixtures() {
    }

    /** A public client, the way most MCP clients register. */
    public static RegisteredClient publicClient(String clientId) {
        return RegisteredClient.withId("id-" + clientId)
                .clientId(clientId)
                .clientName("Test app")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri(REDIRECT_URI)
                .clientSettings(ClientSettings.builder().requireProofKey(true).requireAuthorizationConsent(true).build())
                .build();
    }

    public static UsernamePasswordAuthenticationToken signedIn(String email, int userId) {
        Set<SimpleGrantedAuthority> authorities = Set.of(new SimpleGrantedAuthority("ROLE_USER"));
        DrinkcounterUserDetails details = new DrinkcounterUserDetails(email, "", true, true, true, true, authorities, userId);
        return UsernamePasswordAuthenticationToken.authenticated(details, null, authorities);
    }

    /** An authorization waiting for the user's consent, as the consent page finds it by its state. */
    public static OAuth2Authorization awaitingConsent(RegisteredClient client, String state, String email, int userId) {
        OAuth2AuthorizationRequest request = OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("http://localhost/oauth2/authorize")
                .clientId(client.getClientId())
                .redirectUri(REDIRECT_URI)
                .scopes(client.getScopes())
                .state("client-state")
                .build();
        return OAuth2Authorization.withRegisteredClient(client)
                .principalName(email)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .attribute(Principal.class.getName(), signedIn(email, userId))
                .attribute(OAuth2AuthorizationRequest.class.getName(), request)
                .attribute("state", state)
                .build();
    }
}
