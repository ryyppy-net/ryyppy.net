package drinkcounter.web.controllers;

import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.controllers.api.v2.DrinkDTO;
import drinkcounter.web.controllers.api.v2.DrinkHistoryService;
import drinkcounter.web.controllers.api.v2.ParticipantPreviewDTO;
import drinkcounter.web.controllers.api.v2.PartyDTO;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
        // Embed the data UserCtrl (and the promille history graph it renders)
        // fetches immediately on load - same shapes as GET /API/v2/profile,
        // /API/v2/parties, /API/v2/profile/drinks and
        // /API/v2/profile/drink-history - so it can skip those first fetches.
        // See the comment in app/index.jsp.
        model.addAttribute("initialProfile", toScriptSafeJson(loadInitialProfile()));
        model.addAttribute("initialParties", toScriptSafeJson(loadInitialParties()));
        model.addAttribute("initialDrinks", toScriptSafeJson(loadInitialDrinks()));
        model.addAttribute("initialDrinkHistory", toScriptSafeJson(loadInitialDrinkHistory()));
        return "app/index";
    }

    /**
     * Serializes to JSON safe to embed directly (unescaped) inside a JSP
     * {@code <script>} block. JSTL's usual fn:escapeXml is the wrong tool
     * here: it HTML-entity-encodes quotes, but a browser's HTML parser never
     * decodes entities inside <script> text content, so an entity-encoded
     * JSON string just fails to parse. Escaping "<" instead prevents the
     * output from ever containing a literal "</script" that would close the
     * tag early; U+2028/U+2029 are legal in JSON strings but illegal
     * unescaped in a JS source, where a <script> block is parsed as one.
     */
    private String toScriptSafeJson(Object value) {
        return objectMapper.writeValueAsString(value)
                .replace("<", "\\u003c")
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }

    private UserDTO loadInitialProfile() {
        User user = currentUser.getUser();
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(SlopeService.getSlopes(user));
        return userDTO;
    }

    private List<PartyDTO> loadInitialParties() {
        List<Party> parties = currentUser.getUser().getParties();
        List<PartyDTO> partyDTOs = new ArrayList<>();
        for (Party party : parties) {
            PartyDTO partyDTO = PartyDTO.fromParty(party);
            for (User participant : party.getParticipants()) {
                partyDTO.addParticipant(ParticipantPreviewDTO.fromUser(participant));
            }
            partyDTOs.add(partyDTO);
        }
        return partyDTOs;
    }

    private List<DrinkDTO> loadInitialDrinks() {
        List<Drink> drinks = currentUser.getUser().getDrinks();
        List<DrinkDTO> drinkDTOs = new ArrayList<>();
        for (Drink drink : drinks) {
            DrinkDTO drinkDTO = new DrinkDTO();
            drinkDTO.setId(drink.getId());
            drinkDTO.setTimestamp(drink.getTimeStamp().toString());
            drinkDTO.setAmountOfShots(drink.getAmountOfShots());
            drinkDTOs.add(drinkDTO);
        }
        return drinkDTOs;
    }

    private String loadInitialDrinkHistory() {
        try {
            return DrinkHistoryService.buildCsv(currentUser.getUser().getDrinks(), Clock.systemUTC());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
