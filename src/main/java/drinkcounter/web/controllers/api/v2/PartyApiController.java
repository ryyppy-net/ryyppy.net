/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.google.gson.Gson;
import drinkcounter.DrinkCounterService;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.Party;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
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
}
