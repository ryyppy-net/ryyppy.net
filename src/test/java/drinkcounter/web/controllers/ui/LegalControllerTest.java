package drinkcounter.web.controllers.ui;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Renders the Thymeleaf templates directly (no full Spring context, no
 * datasource) so the JSP-to-Thymeleaf migration's converted pages can be
 * verified without booting the full app against Postgres. A mock servlet
 * request/response stands in for the real DispatcherServlet dispatch, since
 * th:href="@{...}" link expressions require a web context to resolve.
 */
class LegalControllerTest {

    private static String render(String templateName) {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JakartaServletWebApplication webApplication = JakartaServletWebApplication.buildApplication(servletContext);
        WebContext context = new WebContext(webApplication.buildExchange(request, response), Locale.of("fi", "FI"));

        return engine.process(templateName, context);
    }

    @Test
    void controllerReturnsViewNamesMatchingTemplateFiles() {
        LegalController controller = new LegalController();

        assertEquals("privacy", controller.privacyPolicy());
        assertEquals("terms", controller.termsOfService());
    }

    @Test
    void privacyTemplateRendersThroughMasterLayoutWithoutUnresolvedExpressions() {
        String html = render("privacy");

        assertTrue(html.contains("<title>Tietosuojaseloste - Ryyppy.net</title>"), "title from the page wasn't projected into the master layout's <head>");
        assertTrue(html.contains("Tietosuojaseloste"), "page body content missing");
        assertTrue(html.contains("href=\"/static/css/login.css\""), "page-specific stylesheet from customHead missing");
        assertTrue(html.contains("href=\"/static/css/style.css\""), "shared stylesheet from the master layout missing");
        assertTrue(html.contains("src=\"/static/images/logo_ryyppy.png\""), "logo image missing");
        assertFalse(html.contains("${"), "unresolved Thymeleaf expression leaked into output");
        assertFalse(html.contains("customHead") , "the customHead/content wrapper divs should be removed by th:remove=\"tag\"");
    }
}
