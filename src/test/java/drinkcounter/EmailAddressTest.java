package drinkcounter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class EmailAddressTest {

    @Test
    public void acceptsAddressWithDomainAndTld() {
        assertTrue(EmailAddress.isValid("friend@example.com"));
        assertTrue(EmailAddress.isValid("Friend@Example.COM"));
    }

    @Test
    public void rejectsNullEmptyAndMalformed() {
        assertFalse(EmailAddress.isValid(null));
        assertFalse(EmailAddress.isValid(""));
        assertFalse(EmailAddress.isValid("abc"));
        assertFalse(EmailAddress.isValid("abc@example"));
        assertFalse(EmailAddress.isValid("@example.com"));
    }
}
