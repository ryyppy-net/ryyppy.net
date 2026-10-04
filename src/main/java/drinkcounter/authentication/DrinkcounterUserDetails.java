/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 * The principal of form login, Google One Tap and the auth relay.
 *
 * @author Toni
 */
public class DrinkcounterUserDetails extends User implements LoggedInUserPrincipal {

    // Sessions in spring_session hold this class serialized; keep this value so they still deserialize.
    private static final long serialVersionUID = 566890284884385638L;

    private int userId;

    public DrinkcounterUserDetails(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, int userId) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
    }

    @Override
    public LoggedInUser getLoggedInUser() {
        return new LoggedInUser(userId, getUsername());
    }
}
