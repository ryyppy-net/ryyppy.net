package drinkcounter.authentication.authorizationserver;

import java.time.Duration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.server.authorization.OAuth2ClientRegistration;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.converter.OAuth2ClientRegistrationRegisteredClientConverter;
import org.springframework.security.oauth2.server.authorization.settings.OAuth2TokenFormat;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;

/**
 * What any app that registers itself gets, whatever it asks for: the authorization code and
 * refresh token grants, the {@value AuthorizationServerConfiguration#MCP_SCOPE} scope, and opaque
 * tokens. Every other grant would issue a token that acts for no signed-in user.
 */
public class ClientRegistrationPolicy implements Converter<OAuth2ClientRegistration, RegisteredClient> {

    static final Duration ACCESS_TOKEN_TIME_TO_LIVE = Duration.ofHours(1);
    static final Duration REFRESH_TOKEN_TIME_TO_LIVE = Duration.ofDays(30);

    private final OAuth2ClientRegistrationRegisteredClientConverter requested = new OAuth2ClientRegistrationRegisteredClientConverter();

    @Override
    public RegisteredClient convert(OAuth2ClientRegistration registration) {
        return RegisteredClient.from(requested.convert(registration))
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.clear();
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                })
                .scopes(scopes -> {
                    scopes.clear();
                    scopes.add(AuthorizationServerConfiguration.MCP_SCOPE);
                })
                .tokenSettings(TokenSettings.builder()
                        .accessTokenFormat(OAuth2TokenFormat.REFERENCE)
                        .accessTokenTimeToLive(ACCESS_TOKEN_TIME_TO_LIVE)
                        .refreshTokenTimeToLive(REFRESH_TOKEN_TIME_TO_LIVE)
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }
}
