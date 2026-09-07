package drinkcounter.web.controllers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

@RequestMapping("/")
@Controller
public class DefaultController {

    public static final String REDIRECT_TO_FRONTPAGE = "redirect:/app/index.html#/";

    @GetMapping
    public String redirectToFrontPage() {
        return REDIRECT_TO_FRONTPAGE;
    }

    @GetMapping("/app/index.html")
    public String appIndex(Model model) {
        // Pre-populate Angular's $templateCache for the default route's
        // templates so it doesn't have to fetch them over XHR after
        // bootstrap - see the comment in app/index.jsp. Read fresh on every
        // request for now (no caching yet).
        model.addAttribute("userTemplate", readClasspathResource("app/partials/user.html"));
        model.addAttribute("userMenuTemplate", readClasspathResource("app/partials/user_menu.html"));
        model.addAttribute("userButtonTemplate", readClasspathResource("app/partials/user_button.html"));
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
