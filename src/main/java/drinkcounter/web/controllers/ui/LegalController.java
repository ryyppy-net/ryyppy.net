package drinkcounter.web.controllers.ui;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("ui")
public class LegalController {

    @GetMapping("/privacy")
    public String privacyPolicy() {
        return "privacy";
    }

    @GetMapping("/terms")
    public String termsOfService() {
        return "terms";
    }
}
