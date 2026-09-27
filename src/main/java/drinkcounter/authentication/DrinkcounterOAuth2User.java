package drinkcounter.authentication;

import java.util.Collection;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

public class DrinkcounterOAuth2User extends DefaultOAuth2User implements DrinkcounterPrincipal {

    private final int userId;

    public DrinkcounterOAuth2User(Collection<? extends GrantedAuthority> authorities, Map<String, Object> attributes,
            String nameAttributeKey, int userId) {
        super(authorities, attributes, nameAttributeKey);
        this.userId = userId;
    }

    @Override
    public int getUserId() {
        return userId;
    }
}
