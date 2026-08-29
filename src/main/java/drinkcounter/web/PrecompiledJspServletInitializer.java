package drinkcounter.web;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Registers the servlets JspC generated at build time (see pom.xml's
 * exec-maven-plugin execution) directly against each JSP's exact path, so
 * Spring's JSP view forwards hit the precompiled class instead of Tomcat's
 * default JspServlet. Since neither Jasper's compiler nor JspServlet's
 * runtime TLD lookups ever run, this removes both the first-request
 * compilation cost and the dependency on runtime TLD scanning.
 */
@Component
public class PrecompiledJspServletInitializer implements ServletContextInitializer {

    private static final String FRAGMENT_RESOURCE = "/META-INF/jsp-web-fragment.xml";

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        Document fragment = parseFragment();

        Map<String, String> servletClassesByName = new HashMap<>();
        NodeList servlets = fragment.getElementsByTagName("servlet");
        for (int i = 0; i < servlets.getLength(); i++) {
            Element servlet = (Element) servlets.item(i);
            servletClassesByName.put(childText(servlet, "servlet-name"), childText(servlet, "servlet-class"));
        }

        NodeList mappings = fragment.getElementsByTagName("servlet-mapping");
        for (int i = 0; i < mappings.getLength(); i++) {
            Element mapping = (Element) mappings.item(i);
            String name = childText(mapping, "servlet-name");
            String urlPattern = childText(mapping, "url-pattern");
            String className = servletClassesByName.get(name);
            if (className == null) {
                throw new ServletException("Precompiled JSP fragment has no <servlet> entry for " + name);
            }
            servletContext.addServlet(name, className).addMapping(urlPattern);
        }
    }

    private Document parseFragment() throws ServletException {
        try (InputStream in = getClass().getResourceAsStream(FRAGMENT_RESOURCE)) {
            if (in == null) {
                throw new ServletException("Precompiled JSP fragment not found on classpath: " + FRAGMENT_RESOURCE);
            }
            // -webinc emits a bare list of <servlet>/<servlet-mapping> elements, not a
            // full document, so wrap it in a root element before parsing.
            String wrapped = "<fragment>" + new String(in.readAllBytes(), StandardCharsets.UTF_8) + "</fragment>";

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new ByteArrayInputStream(wrapped.getBytes(StandardCharsets.UTF_8)));
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException("Failed to parse precompiled JSP fragment " + FRAGMENT_RESOURCE, e);
        }
    }

    private static String childText(Element parent, String tagName) {
        return parent.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }
}
