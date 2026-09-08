package drinkcounter.web.controllers;

import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.User;
import drinkcounter.web.controllers.api.v2.SlopeService;
import drinkcounter.web.controllers.api.v2.UserDTO;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;
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
    private final CurrentUser currentUser;
    private final ObjectMapper objectMapper;

    public DefaultController(ResourcePatternResolver resourcePatternResolver, CurrentUser currentUser, ObjectMapper objectMapper) {
        this.resourcePatternResolver = resourcePatternResolver;
        this.currentUser = currentUser;
        this.objectMapper = objectMapper;
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
        // Embed the current user's own profile (same shape as GET /API/v2/profile)
        // so UserCtrl can skip its first fetch - see the comment in app/index.jsp.
        model.addAttribute("initialProfile", objectMapper.writeValueAsString(loadInitialProfile()));
        return "app/index";
    }

    private UserDTO loadInitialProfile() {
        User user = currentUser.getUser();
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(SlopeService.getSlopes(user));
        return userDTO;
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
