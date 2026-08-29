/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;

/**
 *
 * @author Toni
 */
public class UserDetailsServiceImpl implements UserDetailsService{

    private final UserService userService;

    public UserDetailsServiceImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException, DataAccessException {
        User user = userService.getUserByEmail(username);
        if (user == null) {
            user = userService.getUserByOpenId(username);
        }
        if(user == null){
            throw new UsernameNotFoundException("User with username "+username+" doesn't exist");
        }
        return new DrinkcounterUserDetails(user.getEmail(),
                user.getPassword(),
                true, 
                true, 
                true, 
                true, 
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                user.getId());
    }
    
}
