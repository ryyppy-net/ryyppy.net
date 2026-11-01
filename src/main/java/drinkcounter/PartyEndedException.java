package drinkcounter;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** A drink was added through a party that has already ended; answered with 409 Conflict. */
@ResponseStatus(value = HttpStatus.CONFLICT, reason = "The party has ended")
public class PartyEndedException extends RuntimeException {

    public PartyEndedException(int partyId) {
        super("Party " + partyId + " has ended");
    }
}
