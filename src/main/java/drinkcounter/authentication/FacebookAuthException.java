/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.authentication;

import java.text.MessageFormat;
import org.springframework.security.core.AuthenticationException;

/**
 *
 * @author Toni
 */
public class FacebookAuthException extends AuthenticationException{
    
    private String error;
    private String errorDescription;
    private String reason;

    public FacebookAuthException(String error, String errorDescription, String reason) {
        super(MessageFormat.format("Facebook authentication failure: {0} Reason: {1} Description: {2}", 
                error, reason, errorDescription));
        this.error = error;
        this.errorDescription = errorDescription;
        this.reason = reason;
        
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getReason() {
        return reason;
    }
}
