package drinkcounter.authentication;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

/** The principal of Google's OpenID Connect login. */
public class DrinkcounterOidcUser extends DefaultOidcUser implements LoggedInUserPrincipal {

    private final LoggedInUser loggedInUser;

    public DrinkcounterOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
            OidcUserInfo userInfo, LoggedInUser loggedInUser) {
        super(authorities, idToken, userInfo, "email");
        this.loggedInUser = loggedInUser;
    }

    @Override
    public LoggedInUser getLoggedInUser() {
        return loggedInUser;
    }
}
