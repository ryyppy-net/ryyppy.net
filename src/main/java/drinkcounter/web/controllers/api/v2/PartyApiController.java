/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.google.gson.Gson;
import drinkcounter.DrinkCounterService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Party;
import drinkcounter.model.User;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("/v2")
public class PartyApiController {
    
    @Autowired
    private CurrentUser currentUser;
    @Autowired
    private DrinkCounterService drinkCounterService;
    
    private Gson gson = new Gson();
    
    @RequestMapping(value="/parties", method= RequestMethod.GET)
    public @ResponseBody String getParties(){
        List<Party> parties = currentUser.getUser().getParties();
        List<PartyDTO> partyDTOs = new ArrayList<PartyDTO>();
        for (Party party : parties) {
            partyDTOs.add(PartyDTO.fromParty(party));
        }
        return gson.toJson(partyDTOs);
    }
    
    @RequestMapping(value="/parties/{partyId}")
    public @ResponseBody String getParty(@PathVariable Integer partyId){
        Party party = drinkCounterService.getParty(partyId);
        PartyDTO partyDTO = PartyDTO.fromParty(party);
        return gson.toJson(partyDTO);
    }
    
    @RequestMapping(value="/parties/{partyId}/participants")
    public @ResponseBody String getParticipants(@PathVariable Integer partyId){
        Party party = drinkCounterService.getParty(partyId);
        List<User> participants = party.getParticipants();
        List<ParticipantDTO> participantDTOs = new ArrayList<ParticipantDTO>();
        for (User participant : participants) {
            participantDTOs.add(ParticipantDTO.fromUser(participant));
        }
        return gson.toJson(participantDTOs);
    }
}
