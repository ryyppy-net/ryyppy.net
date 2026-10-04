package drinkcounter.authentication;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.security.test.context.support.WithSecurityContext;

/**
 * Runs a test as a signed-in user whose principal is a {@link DrinkcounterUserDetails}, the
 * principal every login method in the app produces.
 */
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithDrinkcounterUserSecurityContextFactory.class)
public @interface WithDrinkcounterUser {
    int userId() default 1;

    String email() default "user@example.com";
}
