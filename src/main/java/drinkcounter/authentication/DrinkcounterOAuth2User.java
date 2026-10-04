package drinkcounter.authentication;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

/** The principal of plain OAuth2 login, for providers without OpenID Connect. */
public class DrinkcounterOAuth2User extends DefaultOAuth2User implements LoggedInUserPrincipal {

    private final LoggedInUser loggedInUser;

    public DrinkcounterOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes,
            LoggedInUser loggedInUser) {
        super(authorities, attributes, "email");
        this.loggedInUser = loggedInUser;
    }

    @Override
    public LoggedInUser getLoggedInUser() {
        return loggedInUser;
    }
}
