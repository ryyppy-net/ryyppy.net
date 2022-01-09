/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
            DefaultRedirectStrategy redirectStrategy = new DefaultRedirectStrategy();
            // redirect to registration
            redirectStrategy.sendRedirect(request, response, registrationURL);

        } else {
            super.onAuthenticationFailure(request, response, exception);
        }
    }
}
