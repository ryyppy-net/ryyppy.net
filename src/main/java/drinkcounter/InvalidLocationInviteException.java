package drinkcounter;

/** A location invite's place is not on Earth or its radius is outside the allowed range. */
public class InvalidLocationInviteException extends IllegalArgumentException {

    public InvalidLocationInviteException(String message) {
        super(message);
    }
}
