package drinkcounter.web.controllers;

import drinkcounter.authentication.LoggedInUser;
import drinkcounter.model.Drink;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.util.DrinksPerDayCsv;
import drinkcounter.web.controllers.api.v2.DrinkDTO;
import drinkcounter.web.controllers.api.v2.ParticipantPreviewDTO;
import drinkcounter.web.controllers.api.v2.PartyDTO;
import drinkcounter.web.controllers.api.v2.PromilleHistory;
import drinkcounter.web.controllers.api.v2.UserDTO;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.JavaScriptUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/")
@Controller
public class DefaultController {

    public static final String REDIRECT_TO_FRONTPAGE = "redirect:/app/index.html#!/";

    private final ObjectMapper objectMapper;

    public DefaultController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String redirectToFrontPage() {
        return REDIRECT_TO_FRONTPAGE;
    }

    @GetMapping("/app/index.html")
    public String appIndex(@LoggedInUser User user, Model model) {
        // Dashboard data userPage (and its promille history graph) would
        // otherwise fetch over the API right after load - see templates/app/index.html.
        model.addAttribute("initialProfile", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialProfile(user))));
        model.addAttribute("initialParties", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialParties(user))));
        model.addAttribute("initialDrinks", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialDrinks(user))));
        model.addAttribute("initialDrinkHistory", JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialDrinkHistory(user))));
        return "app/index";
    }

    private UserDTO loadInitialProfile(User user) {
        UserDTO userDTO = UserDTO.fromUser(user);
        userDTO.setHistory(PromilleHistory.forUser(user));
        return userDTO;
    }

    private List<PartyDTO> loadInitialParties(User user) {
        List<Party> parties = user.getParties();
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

    private List<DrinkDTO> loadInitialDrinks(User user) {
        List<Drink> drinks = user.getDrinks();
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

    private String loadInitialDrinkHistory(User user) {
        try {
            return DrinksPerDayCsv.build(user.getDrinks(), Clock.systemUTC());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
