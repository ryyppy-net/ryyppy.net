package drinkcounter.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Allows the handler only when the signed-in user is a member of the party named by its
 * {@code partyId} parameter and either started it or is its only member with an account;
 * anyone else gets 403.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@partyAccess.canDelete(#partyId, principal.userId)")
public @interface PartyStarterOrLastUser {
}
