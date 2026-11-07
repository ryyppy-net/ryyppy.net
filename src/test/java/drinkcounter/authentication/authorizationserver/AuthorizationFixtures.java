package drinkcounter.authentication.authorizationserver;

import drinkcounter.authentication.DrinkcounterUserDetails;
import java.util.Set;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;

/** Apps as the authorization server reads them from their client metadata documents. */
public final class AuthorizationFixtures {

    public static final String REDIRECT_URI = "http://127.0.0.1:33418/callback";

    private AuthorizationFixtures() {
    }

    /** A public client, as most MCP clients' documents describe themselves. */
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
}
