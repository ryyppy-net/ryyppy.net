package drinkcounter.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Allows the handler when its {@code userId} parameter is the signed-in user or someone who shares
 * a party with them; anyone else gets 403. {@code userId} must be numeric, as for {@link OwnUser}.
 * Any party member can add any user to their party, so sharing a party is not a sign of trust.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("#userId == principal.userId or @partyAccess.isPartyMate(principal.userId, #userId)")
public @interface OwnUserOrPartyMate {
}
