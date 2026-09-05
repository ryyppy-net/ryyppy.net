package drinkcounter.authentication;

/** Thrown when a cross-environment Google sign-in handoff token fails verification. */
public class AuthRelayException extends RuntimeException {

    public AuthRelayException(String message) {
        super(message);
    }

    public AuthRelayException(String message, Throwable cause) {
        super(message, cause);
    }
}
