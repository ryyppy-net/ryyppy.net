package drinkcounter.web.controllers.ui;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * Asks the signed-in user whether an app may act as them. Any app can register under any name, so
 * the page also shows where the answer sends the user, which the app cannot fake.
 */
@Controller
public class ConsentController {

    private static final OAuth2TokenType STATE = new OAuth2TokenType(OAuth2ParameterNames.STATE);

    private final RegisteredClientRepository registeredClients;
    private final OAuth2AuthorizationService authorizations;

    public ConsentController(RegisteredClientRepository registeredClients, OAuth2AuthorizationService authorizations) {
        this.registeredClients = registeredClients;
        this.authorizations = authorizations;
    }

    @GetMapping("/ui/consent")
    public ModelAndView consent(@RequestParam(OAuth2ParameterNames.CLIENT_ID) String clientId,
            @RequestParam(OAuth2ParameterNames.STATE) String state, Principal principal, Model model) {
        RegisteredClient client = registeredClients.findByClientId(clientId);
        OAuth2Authorization authorization = authorizations.findByToken(state, STATE);
        OAuth2AuthorizationRequest request = authorization == null ? null
                : authorization.getAttribute(OAuth2AuthorizationRequest.class.getName());
        if (client == null || request == null
                || !client.getId().equals(authorization.getRegisteredClientId())
                || !principal.getName().equals(authorization.getPrincipalName())) {
            return new ModelAndView("consent", model.asMap(), HttpStatus.BAD_REQUEST);
        }
        model.addAttribute("clientId", clientId);
        model.addAttribute("state", state);
        model.addAttribute("clientName", client.getClientName());
        model.addAttribute("redirectsTo", origin(request.getRedirectUri()));
        model.addAttribute("scopes", List.copyOf(request.getScopes()));
        model.addAttribute("email", principal.getName());
        return new ModelAndView("consent", model.asMap());
    }

    private static String origin(String redirectUri) {
        URI uri = URI.create(redirectUri);
        if (uri.getHost() == null) {
            return uri.getScheme() + ":";
        }
        return uri.getScheme() + "://" + uri.getHost() + (uri.getPort() == -1 ? "" : ":" + uri.getPort());
    }
}
