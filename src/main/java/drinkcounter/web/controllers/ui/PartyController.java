/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package drinkcounter.web.controllers.ui;

import drinkcounter.DrinkCounterService;
import drinkcounter.model.Participant;
import org.apache.commons.lang.StringUtils;
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
        return mav;
    }

    @RequestMapping("/viewParty")
    public ModelAndView viewParty(@RequestParam("id") String partyId){
        ModelAndView mav = new ModelAndView();
        mav.setViewName("party");
        mav.addObject("party", drinkCounterService.getParty(partyId));
        mav.addObject("participants", drinkCounterService.listParticipants(partyId));
        return mav;
    }

    @RequestMapping("/addParty")
    public String addParty(@RequestParam("name") String partyName){
        drinkCounterService.startParty(partyName);
        return "redirect:viewParty?id="+partyName;
    }

    @RequestMapping("/addParticipant")
    public String addParticipant(@RequestParam("partyId") String partyId,
            @RequestParam("name") String name,
            @RequestParam("sex") String sex,
            @RequestParam("weight") float weight){
            Participant participant = new Participant();
            participant.setName(name);
            participant.setSex(Participant.Sex.valueOf(sex));
            participant.setWeight(weight);
            drinkCounterService.addParticipant(participant, partyId);
            return "redirect:viewParty?id="+partyId;
    }

    @RequestMapping("/addDrink")
    public String addDrink(@RequestParam("id") String participantId){
        drinkCounterService.addDrink(participantId);
        String partyId = drinkCounterService.getPartyIdForParticipant(participantId);
        return "redirect:viewParty?id="+partyId;
    }
}
