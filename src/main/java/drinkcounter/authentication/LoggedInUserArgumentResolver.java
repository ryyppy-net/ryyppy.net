package drinkcounter.authentication;

import drinkcounter.UserAccounts;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * Resolves {@link LoggedInUser} parameters by loading the user named by the
 * {@link DrinkcounterUserDetails} principal.
 */
public class LoggedInUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final UserAccounts userAccounts;

    public LoggedInUserArgumentResolver(UserAccounts userAccounts) {
        this.userAccounts = userAccounts;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoggedInUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Object principal = authentication == null ? null : authentication.getPrincipal();
        if (!(principal instanceof DrinkcounterUserDetails details)) {
            throw new IllegalStateException("@LoggedInUser needs a DrinkcounterUserDetails principal, got "
                    + (principal == null ? null : principal.getClass().getName()));
        }
        return userAccounts.get(details.getUserId());
    }
}
