package drinkcounter.authentication;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Resolves a controller method parameter to the signed-in user's {@link drinkcounter.model.User}
 * entity, loaded through {@link LoggedInUserArgumentResolver}.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LoggedInUser {}
