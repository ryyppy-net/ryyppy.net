/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import drinkcounter.DrinkCounterServiceImpl;
import drinkcounter.web.controllers.ui.AuthenticationController;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

/**
 *
 * @author Toni
 */
public class UserNotRegisteredFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    
    private static final Logger log = LoggerFactory.getLogger(UserNotRegisteredFailureHandler.class);
    
    private String registrationURL = "/ui/newuser";

    public void setRegistrationURL(String registrationURL) {
        this.registrationURL = registrationURL;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        
        if (exception instanceof UsernameNotFoundException) {
            Authentication authToken = exception.getAuthentication();
            log.info("User authenticated with token {} but has no account. Redirecting to registration...", authToken);
            DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
            request.getSession(true).setAttribute(AuthenticationController.OPENID, authToken);
            // redirect to registration
            redirectStrategy.sendRedirect(request, response, registrationURL);

        } else {
            super.onAuthenticationFailure(request, response, exception);
        }
    }
}
