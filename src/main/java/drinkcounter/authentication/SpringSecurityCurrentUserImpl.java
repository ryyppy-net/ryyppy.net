/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.UserService;
import drinkcounter.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
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
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if(principal instanceof DrinkcounterUserDetails userDetails){
            return userService.getUser(userDetails.getUserId());
        }

        if(principal instanceof OAuth2User oauth2User){
            Integer userId = oauth2User.getAttribute("userId");
            if(userId != null){
                return userService.getUser(userId);
            }
        }

        return null;
    }
}
