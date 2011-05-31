/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import java.util.LinkedList;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 *
 * @author Toni
 */
public class FacebookAuthenticationToken extends AbstractAuthenticationToken{
    
    private UserDetails principal;
    private String profileId;

    public FacebookAuthenticationToken(String profileId) {
        super(new LinkedList<GrantedAuthority>());
        this.profileId = profileId;
    }

    public FacebookAuthenticationToken(UserDetails principal) {
        super(principal.getAuthorities());
        this.principal = principal;
        this.profileId = principal.getUsername();
    }

    public String getProfileId() {
        return profileId;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        if(principal == null){
            return profileId;
        }
        return principal;
    }
    
}
