/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

/**
 *
 * @author Toni
 */
public class FacebookAuthenticationProvider implements AuthenticationProvider{
    
    private UserDetailsService userDetailsService;

    public void setUserDetailsService(UserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        FacebookAuthenticationToken token = (FacebookAuthenticationToken) authentication;
        UserDetails details = userDetailsService.loadUserByUsername(token.getProfileId());
        return new FacebookAuthenticationToken(details);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return FacebookAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
}
