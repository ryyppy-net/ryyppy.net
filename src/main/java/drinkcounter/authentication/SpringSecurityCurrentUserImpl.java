/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 *
 * @author Toni
 */
@Component
public class SpringSecurityCurrentUserImpl implements CurrentUser{
    private final UserService userService;

    public SpringSecurityCurrentUserImpl(UserService userService) {
        this.userService = userService;
    }

    @Override
    public User getUser(){
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(principal instanceof LoggedInUserPrincipal loggedInUserPrincipal){
            return userService.getUser(loggedInUserPrincipal.getLoggedInUser().userId());
        }

        return null;
    }
}
