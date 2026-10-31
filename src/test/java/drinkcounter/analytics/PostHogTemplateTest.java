package drinkcounter.analytics;

import drinkcounter.model.User;
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

/** Renders terms.html, which shares the master layout with the other classic pages. */
class PostHogTemplateTest {

    private static String render(Map<String, Object> model) {
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
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(webApplication.buildExchange(request, response), Locale.of("fi", "FI"));
        model.forEach(context::setVariable);
        return engine.process("terms", context);
    }

    private static Map<String, Object> enabled() {
        Map<String, Object> model = new HashMap<>();
        model.put("posthogKey", "phc_test");
        model.put("posthogEnvironment", "pr-12");
        return model;
    }

    @Test
    void pageHasNoSnippetWithoutAKey() {
        String html = render(new HashMap<>());

        assertFalse(html.contains("posthog"), "analytics is off, the page must not mention PostHog");
    }

    @Test
    void anonymousPageLoadsTheLibraryThroughTheProxyAndTagsTheEnvironment() {
        String html = render(enabled());

        assertTrue(html.contains("script.src = '/ph/static/array.js'"));
        assertTrue(html.contains("posthog.init(\"phc_test\""));
        assertTrue(html.contains("api_host: '/ph'"));
        assertTrue(html.contains("disable_session_recording: true"));
        assertTrue(html.contains("posthog.register({ environment: \"pr-12\"})"));
        assertTrue(html.contains("posthog.capture('$pageview')"));
        assertFalse(html.contains("posthog.identify("));
        assertFalse(html.contains("posthog.reset()"));
        assertFalse(html.contains("${"), "unresolved Thymeleaf expression leaked into output");
    }

    @Test
    void signedInPageIdentifiesTheUserWithTheirProperties() {
        User user = new User();
        user.setId(7);
        user.setName("Ville");
        user.setEmail("ville@example.com");
        user.setSex(User.Sex.FEMALE);
        user.setWeight(62.5f);
        Map<String, Object> model = enabled();
        model.put("user", user);

        String html = render(model);

        assertTrue(html.contains("posthog.identify("));
        assertTrue(html.contains("\"7\""));
        assertTrue(html.contains("\"name\":\"Ville\""));
        assertTrue(html.contains("\"email\":\"ville@example.com\""));
        assertFalse(html.contains("\"sex\""));
        assertFalse(html.contains("\"weight\""));
        assertTrue(html.indexOf("posthog.identify(") < html.indexOf("posthog.capture('$pageview')"),
                "the pageview must be attributed to the identified user");
    }

    @Test
    void signedOutPageResetsThePersonBeforeTheFirstPageview() {
        Map<String, Object> model = enabled();
        model.put("posthogReset", true);

        String html = render(model);

        assertTrue(html.contains("posthog.reset()"));
        assertTrue(html.indexOf("posthog.reset()") < html.indexOf("posthog.register("),
                "reset clears registered properties, so the environment must be registered after it");
        assertTrue(html.indexOf("posthog.reset()") < html.indexOf("posthog.capture('$pageview')"));
    }
}
