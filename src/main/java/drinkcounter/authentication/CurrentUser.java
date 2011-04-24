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
public class CurrentUser {
    @Autowired
    private UserService userService;

    public User getUser(){
        int userId = ((DrinkcounterUserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserId();
        return userService.getUser(userId);
    }
    
}
