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

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders error.html directly (no full Spring context, no datasource), the
 * same way LegalControllerTest does for privacy - except this template is
 * the first in the migration to use #{...}, so a real MessageSource (the
 * app's own messages_fi.properties/messages_en.properties bundles) is wired
 * in to catch wiring mistakes here rather than as a literal "error.title"
 * rendering in the browser.
 */
class ErrorTemplateTest {

    private static String render(String templateName, Locale locale) {
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
        WebContext context = new WebContext(webApplication.buildExchange(request, response), locale);

        return engine.process(templateName, context);
    }

    @Test
    void errorTemplateRendersResolvedFinnishTextThroughMasterLayout() {
        String html = render("error", Locale.of("fi", "FI"));

        assertTrue(html.contains("<title>Ryyppy.net - Virhe!</title>"), "title from the page wasn't projected into the master layout's <head>");
        assertTrue(html.contains("Tapahtui virhe!"), "error.title should resolve to Finnish text");
        assertTrue(html.contains("Mahdollisia syitä:"), "error.reasons should resolve to Finnish text");
        assertTrue(html.contains("sinulla ei riitä oikeudet yrittämällesi sivulle"), "error.list.unauthorized should resolve to Finnish text");
        assertTrue(html.contains("yritit mennä sivulle, jota ei ole olemassa"), "error.list.page_doesnt_exist should resolve to Finnish text");
        assertTrue(html.contains("kerro siitä meille"), "error.list.tell_us should resolve to Finnish text");
        assertTrue(html.contains("etusivulle"), "error.to_frontpage should resolve to Finnish text");
        assertTrue(html.contains("href=\"mailto:ryyppy.net@gmail.com\""), "mailto link should be preserved");
        assertTrue(html.contains("href=\"/\""), "front page link should be preserved");
        assertTrue(html.contains("href=\"/static/css/login.css\""), "page-specific stylesheet from customHead missing");
        assertTrue(html.contains("src=\"/static/js/login.js\""), "page-specific script from customHead missing");
        assertTrue(html.contains("href=\"/static/css/style.css\""), "shared stylesheet from the master layout missing");
        assertTrue(html.contains("src=\"/static/images/logo_ryyppy.png\""), "logo image missing");

        assertFalse(html.contains("error.title"), "raw message key leaked - MessageSource wiring is broken");
        assertFalse(html.contains("${"), "unresolved Thymeleaf expression leaked into output");
        assertFalse(html.contains("customHead"), "the customHead/content wrapper divs should be removed by th:remove=\"tag\"");
    }

    @Test
    void errorTemplateRendersResolvedEnglishText() {
        String html = render("error", Locale.ENGLISH);

        assertTrue(html.contains("Error!"), "error.title should resolve to English text");
        assertTrue(html.contains("Possible causes:"), "error.reasons should resolve to English text");
        assertFalse(html.contains("error.title"), "raw message key leaked - MessageSource wiring is broken");
    }
}
