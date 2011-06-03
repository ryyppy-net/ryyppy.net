package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.authentication.AuthenticationChecks;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;


/**
 *
 * @author Toni
 */
@Controller
public class PartyController {

    @Autowired private DrinkCounterService drinkCounterService;
    @Autowired private UserService userService;
    @Autowired private CurrentUser currentUser;

    @Autowired private AuthenticationChecks authenticationChecks;

    @RequestMapping("/viewParty")
    public ModelAndView viewParty(HttpSession session, @RequestParam("id") String partyId, @RequestParam(value="kick", required=false) String toKick){

        int pid = Integer.parseInt(partyId);
        authenticationChecks.checkRightsForParty(pid);
        
        if (toKick != null)
            drinkCounterService.unlinkUserFromParty(Integer.parseInt(toKick), pid);
        
        ModelAndView mav = new ModelAndView();
        mav.setViewName("party");
        mav.addObject("party", drinkCounterService.getParty(pid));
        mav.addObject("allUsers", userService.listUsers());
        mav.addObject("users", drinkCounterService.listUsersByParty(pid));
        return mav;
    }

    @RequestMapping("/party")
    public ModelAndView party(HttpSession session, @RequestParam("id") String partyId){
        int pid = Integer.parseInt(partyId);
        authenticationChecks.checkRightsForParty(pid);
        User user = currentUser.getUser();

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
   
    @RequestMapping("/linkUserToParty")
    public String linkUserToParty(HttpSession session, @RequestParam("partyId") String partyId,
            @RequestParam("userId") String userId){
                
        int pid = Integer.parseInt(partyId);
        int uid = Integer.parseInt(userId);
        authenticationChecks.checkRightsForParty(pid);
//        authenticationChecks.checkHighLevelRightsToUser(openId, uid); // TODO privacy
        drinkCounterService.linkUserToParty(uid, pid);
        return "redirect:party?id="+partyId;
    }

    @RequestMapping("/removeUserFromParty")
    public @ResponseBody String removeUserFromParty(HttpSession session, @RequestParam("partyId") String partyId,
            @RequestParam("userId") String userId){
        String openId = currentUser.getUser().getOpenId();
        int pid = Integer.parseInt(partyId);
        int uid = Integer.parseInt(userId);
        authenticationChecks.checkRightsForParty(pid);
        authenticationChecks.checkHighLevelRightsToUser(uid);
        drinkCounterService.unlinkUserFromParty(uid, pid);

        return null;
    }
}
