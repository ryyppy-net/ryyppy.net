package drinkcounter.authentication.authorizationserver;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

/**
 * The RSA key that signs access tokens. Tokens outlive a restart only when the key comes from the
 * {@code auth.jwt.signing-key} setting; without it, each start signs with a new key.
 */
public final class SigningKey {

    private SigningKey() {
    }

    public static RSAKey of(String pkcs8Pem) {
        try {
            if (pkcs8Pem == null || pkcs8Pem.isBlank()) {
                return new RSAKeyGenerator(2048).keyIDFromThumbprint(true).generate();
            }
            String base64 = pkcs8Pem.replaceAll("-----(BEGIN|END) PRIVATE KEY-----", "").replaceAll("\\s", "");
            KeyFactory rsa = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) rsa.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
            RSAPublicKey publicKey = (RSAPublicKey) rsa.generatePublic(new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new RSAKey.Builder(publicKey).privateKey(privateKey).keyIDFromThumbprint().build();
        } catch (GeneralSecurityException | JOSEException | IllegalArgumentException | ClassCastException e) {
            throw new IllegalStateException("auth.jwt.signing-key is not a PKCS#8 PEM RSA private key", e);
        }
    }
}
