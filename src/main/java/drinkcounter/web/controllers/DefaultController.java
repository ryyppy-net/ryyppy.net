package drinkcounter.web.controllers;

import drinkcounter.PartyRoster;
import drinkcounter.Promilles;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import drinkcounter.web.controllers.api.v2.OwnDrinksView;
import drinkcounter.web.controllers.api.v2.ParticipantPreviewDTO;
import drinkcounter.web.controllers.api.v2.PartyDTO;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.util.JavaScriptUtils;
import tools.jackson.databind.ObjectMapper;

@RequestMapping("/")
@Controller
public class DefaultController {

    public static final String REDIRECT_TO_FRONTPAGE = "redirect:/app/index.html#!/";
    // PartiesStore first shows 20 parties and asks for one more to know whether to offer the rest.
    private static final int INITIAL_PARTIES = 21;

    private final ObjectMapper objectMapper;
    private final PartyRoster partyRoster;
    private final Promilles promilles;

    public DefaultController(ObjectMapper objectMapper, PartyRoster partyRoster, Promilles promilles) {
        this.objectMapper = objectMapper;
        this.partyRoster = partyRoster;
        this.promilles = promilles;
    }

    @GetMapping
    public String redirectToFrontPage() {
        return REDIRECT_TO_FRONTPAGE;
    }

    @GetMapping("/app/index.html")
    public String appIndex(@LoggedInUser User user, Model model) {
        // Seeds the AngularJS stores, which would otherwise fetch this over the
        // API right after load - see templates/app/index.html.
        model.addAttribute(
                "initialProfile",
                JavaScriptUtils.javaScriptEscape(
                        objectMapper.writeValueAsString(OwnDrinksView.profile(promilles.of(user)))));
        model.addAttribute(
                "initialParties",
                JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(loadInitialParties(user))));
        model.addAttribute(
                "initialDrinks",
                JavaScriptUtils.javaScriptEscape(objectMapper.writeValueAsString(OwnDrinksView.drinks(user))));
        return "app/index";
    }

    private List<PartyDTO> loadInitialParties(User user) {
        List<Party> parties = partyRoster.newestWithParticipantsOf(user.getId(), INITIAL_PARTIES);
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
}
