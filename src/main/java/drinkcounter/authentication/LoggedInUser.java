package drinkcounter.authentication;

import java.io.Serializable;

/** The signed-in user, the same whichever way they signed in. */
public record LoggedInUser(int userId, String email) implements Serializable {
}
