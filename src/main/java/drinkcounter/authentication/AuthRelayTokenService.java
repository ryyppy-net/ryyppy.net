package drinkcounter.authentication;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Signs and verifies the short-lived handoff token that carries a verified Google identity from
 * the hub environment (the one domain actually registered with Google - see login.jsp's
 * oneTapLoginUri) back to whichever environment's login page the sign-in actually started on.
 *
 * Google only accepts pre-registered, exact redirect/login URIs - there's no wildcard and no API
 * to register one per ephemeral Railway PR environment. So every environment points Google's
 * login_uri at the same hub, and the hub hands the verified identity back to the originating
 * environment itself via this token, over a plain redirect, instead of relying on a shared
 * session cookie (which can't cross domains).
 */
@Service
public class AuthRelayTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final long TOKEN_TTL_SECONDS = 60;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, Instant> consumedNonces = new ConcurrentHashMap<>();
    private final String configuredSecret;

    public AuthRelayTokenService() {
        this(System.getenv("AUTH_RELAY_SECRET"));
    }

    /** Visible for testing - lets tests fix a secret instead of depending on the environment. */
    AuthRelayTokenService(String secret) {
        this.configuredSecret = (secret == null || secret.isBlank()) ? null : secret;
    }

    /** Plain data holder for the token's claims. */
    public static class HandoffClaims {
        public String sub;
        public String email;
        public String name;
        public String aud;
        public long exp;
        public String jti;
    }

    /** Whether AUTH_RELAY_SECRET is configured; the relay is inert without it. */
    public boolean isEnabled() {
        return getSecret() != null;
    }

    public String mint(String sub, String email, String name, String audienceOrigin) {
        String secret = requireSecret();

        HandoffClaims claims = new HandoffClaims();
        claims.sub = sub;
        claims.email = email;
        claims.name = name;
        claims.aud = audienceOrigin;
        claims.exp = Instant.now().getEpochSecond() + TOKEN_TTL_SECONDS;
        claims.jti = randomNonce();

        String encodedPayload = base64Url(encodeClaims(claims));
        String signature = sign(encodedPayload, secret);
        return encodedPayload + "." + signature;
    }

    /**
     * Verifies signature, expiry, single-use, and that the token was minted for exactly this
     * origin (so a token intercepted en route can't be replayed against a different environment).
     */
    public HandoffClaims verify(String token, String expectedAudience) {
        String secret = requireSecret();

        int dot = token.indexOf('.');
        if (dot < 0) {
            throw new AuthRelayException("Malformed relay token");
        }
        String encodedPayload = token.substring(0, dot);
        String signature = token.substring(dot + 1);

        if (!constantTimeEquals(signature, sign(encodedPayload, secret))) {
            throw new AuthRelayException("Relay token signature mismatch");
        }

        HandoffClaims claims;
        try {
            claims = decodeClaims(new String(Base64.getUrlDecoder().decode(encodedPayload), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new AuthRelayException("Malformed relay token payload", e);
        }

        if (Instant.now().getEpochSecond() > claims.exp) {
            throw new AuthRelayException("Relay token expired");
        }
        if (!expectedAudience.equalsIgnoreCase(claims.aud)) {
            throw new AuthRelayException("Relay token was issued for a different origin");
        }

        pruneExpiredNonces();
        if (consumedNonces.putIfAbsent(claims.jti, Instant.now()) != null) {
            throw new AuthRelayException("Relay token already used");
        }

        return claims;
    }

    private String sign(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign auth relay token", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private void pruneExpiredNonces() {
        Instant cutoff = Instant.now().minusSeconds(TOKEN_TTL_SECONDS * 2);
        consumedNonces.values().removeIf(seenAt -> seenAt.isBefore(cutoff));
    }

    private String randomNonce() {
        byte[] bytes = new byte[16];
        secureRandom.nextBytes(bytes);
        return base64Url(bytes);
    }

    private String base64Url(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }

    private byte[] encodeClaims(HandoffClaims claims) {
        String encoded = "sub=" + urlEncode(claims.sub)
                + "&email=" + urlEncode(claims.email)
                + "&name=" + urlEncode(claims.name)
                + "&aud=" + urlEncode(claims.aud)
                + "&exp=" + claims.exp
                + "&jti=" + urlEncode(claims.jti);
        return encoded.getBytes(StandardCharsets.UTF_8);
    }

    private HandoffClaims decodeClaims(String encoded) {
        HandoffClaims claims = new HandoffClaims();
        for (String pair : encoded.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String key = pair.substring(0, eq);
            String value = URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8);
            switch (key) {
                case "sub" -> claims.sub = value;
                case "email" -> claims.email = value;
                case "name" -> claims.name = value;
                case "aud" -> claims.aud = value;
                case "exp" -> claims.exp = Long.parseLong(value);
                case "jti" -> claims.jti = value;
                default -> { }
            }
        }
        return claims;
    }

    private String urlEncode(String value) {
        return value == null ? "" : URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String getSecret() {
        return configuredSecret;
    }

    private String requireSecret() {
        String secret = getSecret();
        if (secret == null) {
            throw new IllegalStateException("AUTH_RELAY_SECRET is not configured");
        }
        return secret;
    }
}
