package drinkcounter.authentication;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

public class DrinkcounterOidcUser extends DefaultOidcUser implements DrinkcounterPrincipal {

    private final int userId;

    public DrinkcounterOidcUser(Collection<? extends GrantedAuthority> authorities, OidcIdToken idToken,
            OidcUserInfo userInfo, String nameAttributeKey, int userId) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.userId = userId;
    }

    @Override
    public int getUserId() {
        return userId;
    }
}
