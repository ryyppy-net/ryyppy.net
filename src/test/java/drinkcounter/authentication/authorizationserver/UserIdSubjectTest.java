package drinkcounter.authentication.authorizationserver;

import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.publicClient;
import static drinkcounter.authentication.authorizationserver.AuthorizationFixtures.signedIn;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;

public class UserIdSubjectTest {

    private final UserIdSubject customizer = new UserIdSubject();

    @Test
    public void accessTokenSubjectIsTheUserIdNotTheEmail() {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject("ville@example.com");

        customizer.customize(context(OAuth2TokenType.ACCESS_TOKEN, claims));

        assertEquals("42", claims.build().getSubject());
    }

    @Test
    public void otherTokensAreLeftAlone() {
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder().subject("ville@example.com");

        customizer.customize(context(OAuth2TokenType.REFRESH_TOKEN, claims));

        assertEquals("ville@example.com", claims.build().getSubject());
    }

    private static JwtEncodingContext context(OAuth2TokenType type, JwtClaimsSet.Builder claims) {
        return JwtEncodingContext.with(JwsHeader.with(() -> "RS256"), claims)
                .registeredClient(publicClient("app"))
                .principal(signedIn("ville@example.com", 42))
                .tokenType(type)
                .build();
    }
}
