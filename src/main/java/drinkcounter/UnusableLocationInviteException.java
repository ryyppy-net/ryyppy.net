package drinkcounter;

/**
 * A party that cannot be joined from where the user is: unknown, without a location invite,
 * ended, or out of the invite's radius.
 */
public class UnusableLocationInviteException extends RuntimeException {

    public UnusableLocationInviteException() {
        super("Location invite is not usable");
    }
}
