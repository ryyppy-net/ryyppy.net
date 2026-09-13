package drinkcounter.web.controllers.ui;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders login.html directly (no full Spring context, no datasource), the
 * same way ErrorTemplateTest does for error.html. login.html is the first
 * converted template with real model-driven conditionals - the two mutually
 * exclusive Google sign-in branches and the ?error banner - and the only
 * spring:message with an "arguments=" value in the app, so it gets its own
 * coverage for those specifically rather than relying on manual checks.
 */
class LoginTemplateTest {

    private static String render(Map<String, Object> modelAttributes, Map<String, String> requestParams) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        engine.setTemplateEngineMessageSource(messageSource);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        requestParams.forEach(request::setParameter);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(webApplication.buildExchange(request, response), Locale.of("fi", "FI"));
        modelAttributes.forEach(context::setVariable);

        return engine.process("login", context);
    }

    private static Map<String, Object> defaultModel() {
        Map<String, Object> model = new HashMap<>();
        model.put("totalDrinkCount", 42);
        model.put("googleAuthEnabled", true);
        model.put("isHubEnvironment", true);
        model.put("googleClientId", "test-client-id.apps.googleusercontent.com");
        model.put("oneTapLoginUri", "https://ryyppy.net/api/auth/google/one-tap");
        model.put("useFedCm", false);
        return model;
    }

    @Test
    void hubEnvironmentRendersGsiWidgetNotRelayLink() {
        String html = render(defaultModel(), Map.of());

        assertTrue(html.contains("<title>Ryyppy.net</title>"), "title from the page wasn't projected into the master layout's <head>");
        assertTrue(html.contains("id=\"g_id_onload\""), "GSI One Tap widget should render when isHubEnvironment is true");
        assertTrue(html.contains("data-client_id=\"test-client-id.apps.googleusercontent.com\""), "data-client_id missing from GSI widget");
        assertTrue(html.contains("data-login_uri=\"https://ryyppy.net/api/auth/google/one-tap\""), "data-login_uri missing from GSI widget");
        assertTrue(html.contains("data-auto_select=\"true\""), "data-auto_select should render as the bare string true when ?logout is absent");
        assertTrue(html.contains("data-use_fedcm_for_prompt=\"false\""), "data-use_fedcm_for_prompt should render as the bare string false");
        assertFalse(html.contains("/api/auth/relay/redirect"), "the non-hub relay link should not render alongside the GSI widget");

        assertFalse(html.contains("${"), "unresolved Thymeleaf expression leaked into output");
        assertFalse(html.contains("customHead"), "the customHead/content wrapper divs should be removed by th:remove=\"tag\"");
    }

    @Test
    void nonHubEnvironmentRendersRelayLinkNotGsiWidget() {
        Map<String, Object> model = defaultModel();
        model.put("isHubEnvironment", false);

        String html = render(model, Map.of());

        assertTrue(html.contains("href=\"/api/auth/relay/redirect\""), "the relay link should render when isHubEnvironment is false");
        assertFalse(html.contains("id=\"g_id_onload\""), "the GSI One Tap widget should not render on a non-hub environment");
    }

    @Test
    void googleAuthDisabledRendersNeitherBranch() {
        Map<String, Object> model = defaultModel();
        model.put("googleAuthEnabled", false);

        String html = render(model, Map.of());

        assertFalse(html.contains("id=\"g_id_onload\""), "no GSI widget should render when googleAuthEnabled is false");
        assertFalse(html.contains("/api/auth/relay/redirect"), "no relay link should render when googleAuthEnabled is false");
    }

    @Test
    void drinkCounterArgumentRendersAsRealNumberNotDroppedSilently() {
        String html = render(defaultModel(), Map.of());

        assertTrue(html.contains("42 juomaa juotu"), "login.already_n_drinks argument was dropped - expected the drink count substituted into the message");
    }

    @Test
    void errorParamShowsLoginErrorBanner() {
        String html = render(defaultModel(), Map.of("error", ""));

        assertTrue(html.contains("class=\"loginError\""), ".loginError banner should render on ?error");
        assertTrue(html.contains("Kirjautuminen epäonnistui"), "login.error should resolve to Finnish text");
    }

    @Test
    void noErrorParamHidesLoginErrorBanner() {
        String html = render(defaultModel(), Map.of());

        assertFalse(html.contains("loginError"), ".loginError banner should not render without ?error");
    }

    @Test
    void logoutParamDisablesAutoSelect() {
        String html = render(defaultModel(), Map.of("logout", ""));

        assertTrue(html.contains("data-auto_select=\"false\""), "data-auto_select should be false when ?logout is present");
    }
}
