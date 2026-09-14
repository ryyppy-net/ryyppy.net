package drinkcounter.web;

import drinkcounter.authentication.relay.Origins;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

/**
 * Global controller advice that makes configuration properties available to all views.
 */
@ControllerAdvice
public class GlobalControllerAdvice {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final BuildProperties buildProperties;
    private final boolean fedcmEnabled;
    private final String hubUrl;
    private final SoundManifest soundManifest;

    @Autowired
    public GlobalControllerAdvice(
            ClientRegistrationRepository clientRegistrationRepository,
            @Autowired(required = false) BuildProperties buildProperties,
            @Value("${google.fedcm-enabled:false}") boolean fedcmEnabled,
            @Value("${google.auth.hub-url:}") String hubUrl,
            SoundManifest soundManifest) {
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.buildProperties = buildProperties;
        this.fedcmEnabled = fedcmEnabled;
        this.hubUrl = hubUrl;
        this.soundManifest = soundManifest;
    }

    /**
     * The drink sound clips, as content-hashed URLs. Rendered by
     * fragments/sounds.html into the pages that can add a drink, so sound.js
     * can preload them without a separate request for the list itself. Model
     * attributes are cheap and unused ones are never rendered, so this is on
     * the global advice rather than wired into each of those controllers.
     * See SoundManifest.
     * Available in templates as ${soundUrls}
     */
    @ModelAttribute("soundUrls")
    public List<String> soundUrls() {
        return soundManifest.getSoundUrls();
    }

    /**
     * The app version, sourced from the BuildProperties bean that Spring Boot's
     * ProjectInfoAutoConfiguration derives from META-INF/build-info.properties
     * (see the spring-boot-maven-plugin build-info execution in pom.xml). Not
     * generated when the app is run straight from an IDE without going
     * through that Maven build, so this is null in that case.
     * Available in templates as ${applicationVersion}
     */
    @ModelAttribute("applicationVersion")
    public String applicationVersion() {
        return buildProperties == null ? null : buildProperties.getVersion();
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
     * Returns whether to use FedCM for Google One Tap.
     * Disabled by default, enable with the google.fedcm-enabled property (GOOGLE_FEDCM_ENABLED environment variable).
     * Available in JSP as ${useFedCm}
     */
    @ModelAttribute("useFedCm")
    public boolean useFedCm() {
        return fedcmEnabled;
    }

    /**
     * Returns Google Client ID for One Tap JavaScript.
     * Available in JSP as ${googleClientId}
     */
    @ModelAttribute("googleClientId")
    public String getGoogleClientId() {
        try {
            ClientRegistration registration = clientRegistrationRepository.findByRegistrationId("google");
            if (registration != null && isValidCredential(registration.getClientId())) {
                return registration.getClientId();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    /**
     * The URL Google One Tap POSTs its credential to (data-login_uri in login.jsp).
     *
     * Google only accepts pre-registered login/redirect URIs, so every environment must point
     * this at the same hub domain (the one actually registered in Google Cloud Console) rather
     * than at itself - Railway's ephemeral PR environments get a fresh, unregistered domain every
     * time and could never be registered individually. GoogleOneTapController relays the verified
     * identity back to whichever environment the sign-in actually started on.
     *
     * GOOGLE_AUTH_HUB_URL (e.g. "https://ryyppy.net") configures the hub; unset falls back to
     * this environment's own origin, preserving today's single-environment behavior.
     * Available in JSP as ${oneTapLoginUri}
     */
    @ModelAttribute("oneTapLoginUri")
    public String oneTapLoginUri(HttpServletRequest request) {
        String base = (hubUrl != null && !hubUrl.isBlank())
                ? hubUrl.replaceAll("/+$", "")
                : Origins.of(request);
        return base + "/api/auth/google/one-tap";
    }

    /**
     * Whether this environment is the hub Google is actually registered for.
     *
     * Google Identity Services (One Tap / the styled sign-in button) checks the page's own origin
     * against Google's Authorized JavaScript origins before it lets sign-in proceed - a check that
     * happens entirely on Google's side and can't be relayed. So on any environment that isn't the
     * hub, login.jsp shows a plain link into the classical OAuth2 relay
     * (/api/auth/relay/redirect) instead of the GSI widget, which would otherwise fail there with
     * origin_mismatch.
     *
     * Unset GOOGLE_AUTH_HUB_URL means there's no separate hub configured (e.g. local dev, or a
     * single-environment deployment), so every environment is treated as the hub.
     * Available in JSP as ${isHubEnvironment}
     */
    @ModelAttribute("isHubEnvironment")
    public boolean isHubEnvironment(HttpServletRequest request) {
        if (hubUrl == null || hubUrl.isBlank()) {
            return true;
        }
        return Origins.of(request).equalsIgnoreCase(hubUrl.replaceAll("/+$", ""));
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
