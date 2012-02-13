/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package drinkcounter.web.controllers.api.v2;

import com.google.gson.Gson;
import drinkcounter.DrinkCounterService;
import drinkcounter.UserService;
import drinkcounter.alcoholcalculator.AlcoholCalculator;
import drinkcounter.authentication.CurrentUser;
import drinkcounter.model.User;
import java.util.Collections;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/**
 *
 * @author Toni
 */
@Controller
@RequestMapping("/v2")
public class UserApiController {
    
    @Autowired
    private DrinkCounterService drinkCounterService;
    
    @Autowired
    private CurrentUser currentUser;
    
    private Gson gson = new Gson();
    
    @RequestMapping(value="/profile", method=RequestMethod.GET)
    public @ResponseBody String getUser() {
        UserDTO userDTO = UserDTO.fromUser(currentUser.getUser());
        return gson.toJson(userDTO);
    }
    
    @RequestMapping(value="/profile/drinks", method= RequestMethod.POST)
    public void drink(
            @RequestParam(value="volume", required=false) Float volume,
            @RequestParam(value="alcohol", required=false) Float alcoholPercentage){
        Integer userId = currentUser.getUser().getId();
        if (volume != null && alcoholPercentage != null) {
            float alcoholAmount = AlcoholCalculator.getAlcoholAmount(volume, alcoholPercentage);
            drinkCounterService.addDrink(userId, alcoholAmount);
        } else {
            drinkCounterService.addDrink(userId);
        }
    }
    
}
