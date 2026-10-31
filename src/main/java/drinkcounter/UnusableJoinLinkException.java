package drinkcounter;

/** A join link that is unknown, revoked, replaced or belongs to a party that has ended. */
public class UnusableJoinLinkException extends RuntimeException {

    public UnusableJoinLinkException() {
        super("Join link is not usable");
    }
}
