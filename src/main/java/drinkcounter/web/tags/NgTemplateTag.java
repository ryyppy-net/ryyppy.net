package drinkcounter.web.tags;

import jakarta.servlet.jsp.JspException;
import jakarta.servlet.jsp.JspWriter;
import jakarta.servlet.jsp.tagext.SimpleTagSupport;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Emits a script[type=text/ng-template] block whose id is {@code id} and
 * whose body is the classpath:/public/{@code path} resource, read fresh on
 * every request. Lets a single app/partials/*.html file back both a normal
 * AngularJS templateUrl/ng-include fetch of it and a pre-populated
 * $templateCache entry inlined into the initial page (see index.jsp), so the
 * two never drift apart the way a hand-copied duplicate would.
 */
public class NgTemplateTag extends SimpleTagSupport {

    private String id;
    private String path;

    public void setId(String id) {
        this.id = id;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void doTag() throws JspException, IOException {
        String resourcePath = "public/" + path;
        String content;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new JspException("ngTemplate: classpath resource not found: " + resourcePath);
            }
            content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        JspWriter out = getJspContext().getOut();
        out.write("<script type=\"text/ng-template\" id=\"");
        out.write(id);
        out.write("\">");
        out.write(content);
        out.write("</script>");
    }
}
