package drinkcounter;

import java.util.regex.Pattern;

public final class EmailAddress {

    private static final Pattern FORMAT = Pattern.compile("^(.+)@(.+)\\.(.+)$", Pattern.CASE_INSENSITIVE);

    private EmailAddress() {
    }

    public static boolean isValid(String email) {
        if (email == null || email.length() == 0) return false;

        return FORMAT.matcher(email).matches();
    }
}
