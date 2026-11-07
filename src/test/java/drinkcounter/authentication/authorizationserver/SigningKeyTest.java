package drinkcounter.authentication.authorizationserver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

public class SigningKeyTest {

    @Test
    public void configuredKeyIsUsedWithAKeyIdThatSurvivesARestart() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(generator.generateKeyPair().getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

        RSAKey key = SigningKey.of(pem);

        assertTrue(key.isPrivate());
        assertEquals(key.getKeyID(), SigningKey.of(pem).getKeyID());
        assertEquals(key.toRSAPublicKey(), SigningKey.of(pem).toRSAPublicKey());
    }

    @Test
    public void withoutAConfiguredKeyEachStartSignsWithANewOne() {
        RSAKey key = SigningKey.of("");

        assertTrue(key.isPrivate());
        assertNotEquals(key.getKeyID(), SigningKey.of("").getKeyID());
    }

    @Test
    public void malformedKeyFailsStartup() {
        assertThrows(IllegalStateException.class, () -> SigningKey.of("-----BEGIN PRIVATE KEY-----\nnot a key\n-----END PRIVATE KEY-----"));
    }
}
