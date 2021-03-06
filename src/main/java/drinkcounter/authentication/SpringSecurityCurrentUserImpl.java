/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author Toni
 */
@Component
public class SpringSecurityCurrentUserImpl implements CurrentUser{
    @Autowired
    private UserService userService;

    @Override
    public User getUser(){
        if(SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof DrinkcounterUserDetails){
            int userId = ((DrinkcounterUserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
            return userService.getUser(userId);
        }
        return null;
    }
}
