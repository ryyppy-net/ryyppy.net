package drinkcounter.web.controllers;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

@RequestMapping("/")
@Controller
public class DefaultController {

    public static final String REDIRECT_TO_FRONTPAGE = "redirect:/app/index.html#/";

    private final ResourcePatternResolver resourcePatternResolver;

    public DefaultController(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @GetMapping
    public String redirectToFrontPage() {
        return REDIRECT_TO_FRONTPAGE;
    }

    @GetMapping("/app/index.html")
    public String appIndex(Model model) {
        // Pre-populate Angular's $templateCache for every route's template so
        // it doesn't have to fetch any of them over XHR after bootstrap - see
        // the comment in app/index.jsp. Read fresh on every request for now
        // (no caching yet).
        model.addAttribute("templates", loadPartialTemplates());
        return "app/index";
    }

    private Map<String, String> loadPartialTemplates() {
        Map<String, String> templates = new LinkedHashMap<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:public/app/partials/*.html");
            for (Resource resource : resources) {
                templates.put("partials/" + resource.getFilename(), resource.getContentAsString(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return templates;
    }
}
