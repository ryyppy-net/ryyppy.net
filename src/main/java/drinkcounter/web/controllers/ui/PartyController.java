package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.authentication.LoggedInUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import jakarta.servlet.http.HttpSession;
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
    private final AuthenticationChecks authenticationChecks;

    public PartyController(DrinkCounterService drinkCounterService, AuthenticationChecks authenticationChecks) {
        this.drinkCounterService = drinkCounterService;
        this.authenticationChecks = authenticationChecks;
    }

    @RequestMapping("/party")
    public ModelAndView party(HttpSession session, @LoggedInUser User user, @RequestParam("id") String partyId){
        int pid = Integer.parseInt(partyId);
        authenticationChecks.checkRightsForParty(pid);

        ModelAndView mav = new ModelAndView();
        mav.setViewName("party");
        mav.addObject("party", drinkCounterService.getParty(pid));
        mav.addObject("user", user);
        return mav;
    }

    @RequestMapping("/addParty")
    public String addParty(HttpSession session, @RequestParam("name") String partyName, @RequestParam("userId") String userId){
        int uid = Integer.parseInt(userId);
        authenticationChecks.checkLowLevelRightsToUser(uid);
        
        Party party = drinkCounterService.startParty(partyName);
        drinkCounterService.linkUserToParty(uid, party.getId());
        return "redirect:party?id="+party.getId();
    }
   
    @RequestMapping("/removeUserFromParty")
    public String removeUserFromParty(HttpSession session, @RequestParam("partyId") String partyId,
            @RequestParam("userId") String userId){
        int pid = Integer.parseInt(partyId);
        int uid = Integer.parseInt(userId);
        authenticationChecks.checkRightsForParty(pid);
        authenticationChecks.checkHighLevelRightsToUser(uid);
        drinkCounterService.unlinkUserFromParty(uid, pid);

        return "redirect:user";
    }
}
