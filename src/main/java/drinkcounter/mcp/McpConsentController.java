package drinkcounter.mcp;

import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

/** The page where a signed-in user allows or denies an MCP client access to their account. */
@Controller
public class McpConsentController {

    private final McpConsentRequests consentRequests;

    public McpConsentController(McpConsentRequests consentRequests) {
        this.consentRequests = consentRequests;
    }

    @GetMapping(McpSecurityConfiguration.CONSENT_PAGE)
    public String consent(@RequestParam(name = "state", defaultValue = "") String state, Principal principal, Model model) {
        model.addAttribute("consent", consentRequests.find(state, principal.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST)));
        model.addAttribute("account", principal.getName());
        return "mcp-consent";
    }
}
