package drinkcounter.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Allows the handler only when the signed-in user is a participant of the party named by its
 * {@code partyId} parameter; anyone else gets 403.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@partyAccess.isMember(#partyId, principal.userId)")
public @interface PartyMember {
}
