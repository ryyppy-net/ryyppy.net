package drinkcounter.web.controllers.api.v2;

import drinkcounter.model.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;

public class GravatarUrls {
    public static String forUser(User user) {
        if(user.getEmail() != null) {
            return forValue(user.getEmail());
        }
        else {
            return forValue(user.getName() + user.getWeight());
        }
    }

    private static String forValue(String value) {
        return "https://www.gravatar.com/avatar/" + md5Hex(value.trim().toLowerCase(Locale.ROOT)) + ".jpg?d=wavatar";
    }

    private static String md5Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
