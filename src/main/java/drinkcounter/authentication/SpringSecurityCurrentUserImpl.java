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

        // Handle form login (username/password)
        if(principal instanceof DrinkcounterUserDetails){
            int userId = ((DrinkcounterUserDetails) principal).getUserId();
            return userService.getUser(userId);
        }

        // Handle OAuth2 login (Google) - OIDC
        if(principal instanceof OAuth2User){
            OAuth2User oauth2User = (OAuth2User) principal;
            Integer userId = oauth2User.getAttribute("userId");
            if(userId != null){
                return userService.getUser(userId);
            }
        }

        return null;
    }
}
