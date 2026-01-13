package drinkcounter.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Global controller advice that makes configuration properties available to all views.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public GlobalControllerAdvice(ClientRegistrationRepository clientRegistrationRepository) {
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

    /**
     * Checks if Google OAuth2 login is enabled.
     * Available in JSP as ${googleAuthEnabled}
     */
    @ModelAttribute("googleAuthEnabled")
    public boolean isGoogleAuthEnabled() {
        return isProviderEnabled("google");
    }

    /**
     * Checks if GitHub OAuth2 login is enabled.
     * Available in JSP as ${githubAuthEnabled}
     */
    @ModelAttribute("githubAuthEnabled")
    public boolean isGithubAuthEnabled() {
        return isProviderEnabled("github");
    }

    /**
     * Checks if Facebook OAuth2 login is enabled.
     * Available in JSP as ${facebookAuthEnabled}
     */
    @ModelAttribute("facebookAuthEnabled")
    public boolean isFacebookAuthEnabled() {
        return isProviderEnabled("facebook");
    }

    /**
     * Checks if a specific OAuth2 provider is properly configured.
     *
     * A provider is considered enabled if:
     * - It has a client registration in application.yml
     * - client-id is not empty or a placeholder value
     * - client-secret is not empty or a placeholder value
     */
    private boolean isProviderEnabled(String registrationId) {
        try {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId(registrationId);
            if (registration == null) {
                return false;
            }

            String clientId = registration.getClientId();
            String clientSecret = registration.getClientSecret();

            return isValidCredential(clientId) && isValidCredential(clientSecret);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks if a credential value is valid (not empty, not a placeholder).
     */
    private boolean isValidCredential(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }

        String upper = value.toUpperCase();
        // Check for common placeholder values
        return !upper.contains("REPLACE") &&
               !upper.contains("CHANGEME") &&
               !upper.contains("TODO") &&
               !upper.equals("XXX");
    }
}
