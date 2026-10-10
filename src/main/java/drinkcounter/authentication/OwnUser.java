package drinkcounter.authentication;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Allows the handler only when its {@code userId} parameter is the signed-in user; anyone else
 * gets 403. {@code userId} must be numeric: a {@code String} id never equals the principal's.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("#userId == principal.userId")
public @interface OwnUser {
}
