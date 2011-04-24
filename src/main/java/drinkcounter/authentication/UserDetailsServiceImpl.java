/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 *
 * @author Toni
 */
public class UserDetailsServiceImpl implements UserDetailsService{
    
    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String openId) throws UsernameNotFoundException, DataAccessException {
        User user = userService.getUserByOpenId(openId);
        if(user == null){
            throw new UsernameNotFoundException("User with openid "+openId+" doesn't exist", openId);
        }
        return new org.springframework.security.core.userdetails.User(user.getOpenId(), 
                "", 
                true, 
                true, 
                true, 
                true, 
                Collections.singleton(new GrantedAuthorityImpl("ROLE_USER")));
    }
    
}
