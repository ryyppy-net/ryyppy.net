package drinkcounter.authentication;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class AuthRelayTokenServiceTest {

    private static final String ORIGIN = "https://pr-123.up.railway.app";

    @Test
    public void isEnabledIsFalseAndOperationsRefuseWithoutASecret() {
        AuthRelayTokenService tokenService = withSecret(null);

        assertFalse(tokenService.isEnabled());
        assertThrows(IllegalStateException.class,
                () -> tokenService.mint("sub", "a@b.com", "A B", ORIGIN));
    }

    @Test
    public void mintedTokenVerifiesForItsOwnAudience() {
        AuthRelayTokenService tokenService = withSecret("test-secret");

        String token = tokenService.mint("google-sub", "user@example.com", "User Name", ORIGIN);
        AuthRelayTokenService.HandoffClaims claims = tokenService.verify(token, ORIGIN);

        assertEquals("google-sub", claims.sub);
        assertEquals("user@example.com", claims.email);
        assertEquals("User Name", claims.name);
        assertEquals(ORIGIN, claims.aud);
    }

    @Test
    public void tokenIsRejectedForADifferentOrigin() {
        AuthRelayTokenService tokenService = withSecret("test-secret");
        String token = tokenService.mint("google-sub", "user@example.com", "User Name", ORIGIN);

        assertThrows(AuthRelayException.class,
                () -> tokenService.verify(token, "https://attacker.up.railway.app"));
    }

    @Test
    public void tokenCannotBeReplayed() {
        AuthRelayTokenService tokenService = withSecret("test-secret");
        String token = tokenService.mint("google-sub", "user@example.com", "User Name", ORIGIN);

        tokenService.verify(token, ORIGIN);
        assertThrows(AuthRelayException.class, () -> tokenService.verify(token, ORIGIN));
    }

    @Test
    public void tamperedPayloadIsRejected() {
        AuthRelayTokenService tokenService = withSecret("test-secret");
        String token = tokenService.mint("google-sub", "user@example.com", "User Name", ORIGIN);
        int dot = token.indexOf('.');
        String tampered = token.substring(0, dot) + "x" + token.substring(dot);

        assertThrows(AuthRelayException.class, () -> tokenService.verify(tampered, ORIGIN));
    }

    @Test
    public void tokenSignedWithADifferentSecretIsRejected() {
        String token = withSecret("secret-a").mint("google-sub", "user@example.com", "User Name", ORIGIN);

        assertThrows(AuthRelayException.class, () -> withSecret("secret-b").verify(token, ORIGIN));
    }

    @Test
    public void malformedTokenIsRejectedRatherThanThrowingUnchecked() {
        AuthRelayTokenService tokenService = withSecret("test-secret");

        assertThrows(AuthRelayException.class, () -> tokenService.verify("not-a-real-token", ORIGIN));
    }

    /** Uses the package-private test constructor to fix a secret instead of relying on the environment. */
    private static AuthRelayTokenService withSecret(String secret) {
        return new AuthRelayTokenService(secret);
    }
}
