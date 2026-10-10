package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.authentication.OwnUser;
import drinkcounter.authentication.PartyMember;
import drinkcounter.authentication.PartyMemberAndOwnUserOrPartyMate;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("ui")
public class PartyController {

    private final DrinkCounterService drinkCounterService;

    public PartyController(DrinkCounterService drinkCounterService) {
        this.drinkCounterService = drinkCounterService;
    }

    @RequestMapping("/party")
    @PartyMember
    public ModelAndView party(@LoggedInUser User user, @RequestParam("id") int partyId){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("party");
        mav.addObject("party", drinkCounterService.getParty(partyId));
        mav.addObject("user", user);
        return mav;
    }

    @RequestMapping("/addParty")
    @OwnUser
    public String addParty(@RequestParam("name") String partyName, @RequestParam("userId") int userId){
        Party party = drinkCounterService.startParty(partyName);
        drinkCounterService.linkUserToParty(userId, party.getId());
        return "redirect:party?id="+party.getId();
    }
   
    @RequestMapping("/removeUserFromParty")
    @PartyMemberAndOwnUserOrPartyMate
    public String removeUserFromParty(@RequestParam("partyId") int partyId,
            @RequestParam("userId") int userId){
        drinkCounterService.unlinkUserFromParty(userId, partyId);

        return "redirect:user";
    }
}
