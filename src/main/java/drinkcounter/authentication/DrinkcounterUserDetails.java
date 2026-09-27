/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

/**
 *
 * @author Toni
 */
public class DrinkcounterUserDetails extends User implements DrinkcounterPrincipal {
    // Sessions are serialized to JDBC and live for a year; keeps them readable across changes to this class.
    private static final long serialVersionUID = 566890284884385638L;

    private int userId;

    public DrinkcounterUserDetails(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, int userId) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.userId = userId;
    }

    @Override
    public int getUserId() {
        return userId;
    }
    
}
