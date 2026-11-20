package drinkcounter.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Allows the handler only when the signed-in user may delete the party named by its
 * {@code partyId} parameter: they started it and are still a member, or they are its only user
 * left; anyone else gets 403.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@partyAccess.canDelete(#partyId, principal.userId)")
public @interface PartyStarterOrOnlyUser {
}
