package drinkcounter.web;

import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Sends the {@code XSRF-TOKEN} cookie and {@code X-XSRF-TOKEN} header on every request of the autowired
 * {@code MockMvc}, so a controller test can post as the app's own pages do. {@link CsrfProtectionWebTest}
 * builds its own {@code MockMvc} to see requests without them. Spring Security's {@code csrf()} request
 * post-processor is avoided: it replaces the token repository of the filter chain every test shares.
 */
@TestConfiguration(proxyBeanMethods = false)
public class CsrfTokenConfiguration {

    private static final String TOKEN = "0b6f1e0e-6c39-4b7e-9d1a-3f4c9a6a2f10";

    private static final RequestPostProcessor TOKEN_COOKIE_AND_HEADER = request -> {
        List<Cookie> cookies = new ArrayList<>();
        if (request.getCookies() != null) {
            cookies.addAll(List.of(request.getCookies()));
        }
        cookies.add(new Cookie("XSRF-TOKEN", TOKEN));
        request.setCookies(cookies.toArray(Cookie[]::new));
        request.addHeader("X-XSRF-TOKEN", TOKEN);
        return request;
    };

    @Bean
    MockMvcBuilderCustomizer csrfTokenOnEveryRequest() {
        return builder -> builder.defaultRequest(get("/").with(TOKEN_COOKIE_AND_HEADER));
    }
}
