package drinkcounter.web.controllers.ui;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The page that tells signed-in users how to connect an MCP client. */
@Controller
@RequestMapping("ui")
public class McpGuideController {

    @GetMapping("/mcp")
    public String guide(HttpServletRequest request, Model model) {
        model.addAttribute("mcpUrl", ServletUriComponentsBuilder.fromContextPath(request).path("/mcp").toUriString());
        return "mcp";
    }
}
