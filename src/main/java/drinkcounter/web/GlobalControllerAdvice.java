package drinkcounter.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that makes configuration properties available to all views.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${google.auth.enabled:false}")
    private boolean googleAuthEnabled;

    /**
     * Makes the Google auth enabled flag available to all JSP pages as ${googleAuthEnabled}
     */
    @ModelAttribute("googleAuthEnabled")
    public boolean addGoogleAuthFlag() {
        return googleAuthEnabled;
    }
}
