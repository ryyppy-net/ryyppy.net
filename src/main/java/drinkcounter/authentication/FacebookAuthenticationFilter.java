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
import org.springframework.social.facebook.api.Facebook;
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
        checkFailedAuthentication(request);
        String authorizationGrant = request.getParameter("code");
        if (StringUtils.isBlank(authorizationGrant)) {
            // Get a request token
            OAuth2Parameters parameters = new OAuth2Parameters();
            parameters.setRedirectUri(buildReturnToUrl(request));
            String oauth2Url = facebookConnectionFactory.getOAuthOperations().buildAuthorizeUrl(GrantType.AUTHORIZATION_CODE, parameters);
            response.sendRedirect(oauth2Url);

            // Returning null signals that authentication is not complete
            return null;
        }
        
        // Request an access token
        AccessGrant accessToken = facebookConnectionFactory.getOAuthOperations().exchangeForAccess(authorizationGrant, buildReturnToUrl(request), null);
        Connection<Facebook> connection = facebookConnectionFactory.createConnection(accessToken);
        Facebook facebookApi = connection.getApi();
        String accountId = facebookApi.userOperations().getUserProfile().getId();
        FacebookAuthenticationToken token = new FacebookAuthenticationToken(accountId);
        return getAuthenticationManager().authenticate(token);
    }

    private void checkFailedAuthentication(HttpServletRequest request) throws FacebookAuthException{
        if (request.getParameter("error") != null) {
            throw new FacebookAuthException(
                    request.getParameter("error"),
                    request.getParameter("error_description"),
                    request.getParameter("error_reason"));
        }
    }
    
    private String buildReturnToUrl(HttpServletRequest request){
        return request.getRequestURL().toString();
    }
}
