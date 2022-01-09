package drinkcounter.web.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/")
@Controller
public class DefaultController {

    @GetMapping
    public String redirectToFrontPage() {
        return "redirect:/frontpage.jsp";
    }
}
