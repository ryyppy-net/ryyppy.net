package drinkcounter.web.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequestMapping("/")
@Controller
public class DefaultController {

    public static final String REDIRECT_TO_FRONTPAGE = "redirect:/app/index.html#/";

    // Every templateUrl / ng-include id the AngularJS app uses (see app/js/app.js's
    // $routeProvider and the ng-include calls inside these same partials).
    private static final List<String> TEMPLATE_IDS = List.of(
            "partials/user.html",
            "partials/user_menu.html",
            "partials/user_button.html",
            "partials/profile_settings.html",
            "partials/party.html",
            "partials/party_menu.html",
            "partials/party_admin_general.html",
            "partials/party_admin.html"
    );

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
        Map<String, String> templates = new LinkedHashMap<>();
        for (String id : TEMPLATE_IDS) {
            templates.put(id, readClasspathResource("app/" + id));
        }
        model.addAttribute("templates", templates);
        return "app/index";
    }

    private static String readClasspathResource(String path) {
        try {
            return new ClassPathResource("public/" + path).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
