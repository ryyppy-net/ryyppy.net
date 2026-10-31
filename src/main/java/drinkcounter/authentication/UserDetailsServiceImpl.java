/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.UserAccounts;
import drinkcounter.model.User;
import java.util.Collections;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 *
 * @author Toni
 */
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAccounts userAccounts;

    public UserDetailsServiceImpl(UserAccounts userAccounts) {
        this.userAccounts = userAccounts;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException, DataAccessException {
        User user = userAccounts.byEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("User with username " + email + " doesn't exist");
        }
        return new DrinkcounterUserDetails(
                user.getEmail(),
                user.getPassword(),
                true,
                true,
                true,
                true,
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                user.getId());
    }
}
