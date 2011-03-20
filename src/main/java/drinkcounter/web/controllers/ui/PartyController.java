package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;


/**
 *
 * @author Toni
 */
@Controller
public class PartyController {

    @Autowired private DrinkCounterService drinkCounterService;

    @RequestMapping("/parties")
    public ModelAndView parties() {
        ModelAndView mav = new ModelAndView();
        mav.setViewName("parties");
        mav.addObject("parties", drinkCounterService.listParties());
        mav.addObject("users", drinkCounterService.listUsers());
        return mav;
    }

    @RequestMapping("/viewParty")
    public ModelAndView viewParty(@RequestParam("id") String partyId){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("party");
        mav.addObject("party", drinkCounterService.getParty(partyId));
        mav.addObject("allUsers", drinkCounterService.listUsers());
        mav.addObject("users", drinkCounterService.listUsersByParty(partyId));
        return mav;
    }

    @RequestMapping("/addParty")
    public String addParty(@RequestParam("name") String partyName){
        drinkCounterService.startParty(partyName);
        return "redirect:viewParty?id="+partyName;
    }

    @RequestMapping("/addUser")
    public String addUser(
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight){
            User user = new User();
            user.setName(name);
            user.setSex(User.Sex.valueOf(sex));
            user.setWeight(weight);
            drinkCounterService.addUser(user);
            return "redirect:parties";
    }
    
    @RequestMapping("/addAnonymousUser")
    public String addAnonymousUser(
            @RequestParam("partyId") String partyId,
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight){
            User user = new User();
            user.setName(name);
            user.setSex(User.Sex.valueOf(sex));
            user.setWeight(weight);
            user.setGuest(true);
            drinkCounterService.addUser(user);
            drinkCounterService.linkUserToParty(user.getId(), partyId);
            return "redirect:viewParty?id=" + partyId;
    }
    
    @RequestMapping("/linkUserToParty")
    public String linkUserToParty(@RequestParam("partyId") String partyId,
            @RequestParam("userId") String userId){
            drinkCounterService.linkUserToParty(userId, partyId);
            return "redirect:viewParty?id="+partyId;
    }

    @RequestMapping("/addDrink")
    public String addDrink(@RequestParam("id") String userId){
        drinkCounterService.addDrink(userId);
        return "redirect:parties";
    }
}
