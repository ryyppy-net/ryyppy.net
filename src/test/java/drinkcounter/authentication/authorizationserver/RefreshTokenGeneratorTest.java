package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;

public class RefreshTokenGeneratorTest {

    private final RefreshTokenGenerator generator = new RefreshTokenGenerator();

    @Test
    public void publicClientGetsARefreshTokenForItsSettings() {
        RegisteredClient client = RegisteredClient.from(publicClient("app"))
                .tokenSettings(TokenSettings.builder().refreshTokenTimeToLive(Duration.ofDays(30)).build())
                .build();

        OAuth2RefreshToken token = generator.generate(context(client, OAuth2TokenType.REFRESH_TOKEN));

        assertNotNull(token);
        assertEquals(Duration.ofDays(30), Duration.between(token.getIssuedAt(), token.getExpiresAt()));
        assertNotEquals(token.getTokenValue(), generator.generate(context(client, OAuth2TokenType.REFRESH_TOKEN)).getTokenValue());
    }

    @Test
    public void clientWithoutTheRefreshGrantGetsNone() {
        RegisteredClient client = RegisteredClient.from(publicClient("app"))
                .authorizationGrantTypes(grants -> grants.remove(AuthorizationGrantType.REFRESH_TOKEN))
                .build();

        assertNull(generator.generate(context(client, OAuth2TokenType.REFRESH_TOKEN)));
    }

    @Test
    public void otherTokenTypesAreLeftToOtherGenerators() {
        assertNull(generator.generate(context(publicClient("app"), OAuth2TokenType.ACCESS_TOKEN)));
    }

    private static DefaultOAuth2TokenContext context(RegisteredClient client, OAuth2TokenType type) {
        return DefaultOAuth2TokenContext.builder()
                .registeredClient(client)
                .tokenType(type)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .build();
    }
}
