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
import org.springframework.web.util.JavaScriptUtils;

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
        // Dashboard data UserCtrl (and its promille history graph) would
        // otherwise fetch over the API right after load - see app/index.jsp.
        model.addAttribute("initialProfile", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialProfile())));
        model.addAttribute("initialParties", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialParties())));
        model.addAttribute("initialDrinks", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialDrinks())));
        model.addAttribute("initialDrinkHistory", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialDrinkHistory())));
        return "app/index";
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
