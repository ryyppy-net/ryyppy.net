package drinkcounter.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Both {@link PartyMember} and {@link OwnUserOrPartyMate}: the signed-in user is a participant of
 * the party named by the handler's {@code partyId} parameter, and its {@code userId} parameter is
 * themself or someone who shares a party with them. Anyone else gets 403.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@partyAccess.isMember(#partyId, principal.userId)"
        + " and (#userId == principal.userId or @partyAccess.isPartyMate(principal.userId, #userId))")
public @interface PartyMemberAndOwnUserOrPartyMate {
}
