/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.social.connect.Connection;
import org.springframework.social.facebook.api.FacebookApi;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.social.oauth2.AccessGrant;
import org.springframework.social.oauth2.GrantType;
import org.springframework.social.oauth2.OAuth2Parameters;

/**
 *
 * @author Toni
 */
public class FacebookAuthenticationFilter extends AbstractAuthenticationProcessingFilter{

    private FacebookConnectionFactory facebookConnectionFactory;
    
    public FacebookAuthenticationFilter() {
        super("/j_facebook_check");
    }
    
    public void setFacebookConnectionFactory(FacebookConnectionFactory facebookConnectionFactory) {
        this.facebookConnectionFactory = facebookConnectionFactory;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException, IOException, ServletException {
        String returnToUrl = request.getRequestURL().toString();
        String authorizationGrant = request.getParameter("code");
        if (StringUtils.isBlank(authorizationGrant)) {
            // Get a request token
            String oauth2Url = facebookConnectionFactory.getOAuthOperations().buildAuthorizeUrl(GrantType.AUTHORIZATION_CODE, new OAuth2Parameters(returnToUrl));
            response.sendRedirect(oauth2Url);

            // Returning null signals that authentication is not complete
            return null;
        }
        
        // Request an access token
        AccessGrant accessToken = facebookConnectionFactory.getOAuthOperations().exchangeForAccess(authorizationGrant, returnToUrl, null);
        Connection<FacebookApi> connection = facebookConnectionFactory.createConnection(accessToken);
        FacebookApi facebookApi = connection.getApi();
        String accountId = facebookApi.userOperations().getUserProfile().getId();
        FacebookAuthenticationToken token = new FacebookAuthenticationToken(accountId);
        return getAuthenticationManager().authenticate(token);
    }
    
}
